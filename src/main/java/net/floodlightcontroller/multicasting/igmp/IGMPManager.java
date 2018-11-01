package net.floodlightcontroller.multicasting.igmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.multicasting.IMulticastListener;
import net.floodlightcontroller.multicasting.IMulticastService;
import net.floodlightcontroller.multicasting.internal.ParticipantGroupAddress;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IGMP;
import net.floodlightcontroller.packet.IGMP.IGMPv3GroupRecord;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.util.OFMessageUtils;
import net.floodlightcontroller.packet.IPv4;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Manager for IGMP based Multicasting
 * 
 */ 
public class IGMPManager implements IFloodlightModule, IOFMessageListener, IMulticastListener {
	protected static final Logger log = LoggerFactory.getLogger(IGMPManager.class);
	
	protected IFloodlightProviderService floodlightProvider;

	protected IMulticastService multicastingService;
	
	// Processes PacketIn Message
	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx) {
		// OFVersion version = msg.getVersion();
		OFPort inPort = OFMessageUtils.getInPort(msg);
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		EthType ethType = eth.getEtherType();
		
		MacAddress sourceMACAddress = eth.getSourceMACAddress();
		MacAddress destMACAddress = eth.getDestinationMACAddress();
		
		if (sourceMACAddress == null) {
			return Command.CONTINUE;
		}
		
		if (destMACAddress == null) {
			destMACAddress = MacAddress.NONE;
		}
		
		// Ignoring 802.1Q/D packets
		if ((destMACAddress.getLong() & 0xfffffffffff0L) == 0x0180c2000000L) {
			return Command.CONTINUE;
		}
        
		// Retrieve VlanVid
		VlanVid vlanVid = null; 
        if (msg.getVersion().compareTo(OFVersion.OF_11) > 0 && /* 1.0 and 1.1 do not have a match */
                msg.getMatch().get(MatchField.VLAN_VID) != null) { 
        	vlanVid = msg.getMatch().get(MatchField.VLAN_VID).getVlanVid(); /* VLAN may have been popped by switch */
        }
        if (vlanVid == null) {
        	vlanVid = VlanVid.ofVlan(eth.getVlanID()); /* VLAN might still be in packet */
        }
        
		if (ethType.equals(EthType.IPv4)) {
        	IPv4 ipv4 = (IPv4) eth.getPayload();
        	IpProtocol ipProtocol = ipv4.getProtocol();
        	
    		// IPv4Address srcIPAddress = ipv4.getSourceAddress();
    		IPv4Address destIPAddress = ipv4.getDestinationAddress();
        	
        	if (ipProtocol.equals(IpProtocol.IGMP)) {
        		IGMP igmp = (IGMP) ipv4.getPayload();
        		
        		MacVlanPair srcIntf = new MacVlanPair(sourceMACAddress, vlanVid);
        		NodePortTuple ap = new NodePortTuple(sw.getId(), inPort);
        		
        		if (igmp.isIGMPv3MembershipReportMessage()) {
    				IGMPv3GroupRecord[] groupRecords = igmp.getGroupRecords();
    				
    				for (IGMPv3GroupRecord groupRecord: groupRecords) {
    					IPv4Address multicastIPAddress = groupRecord.getMulticastAddress();
    					ParticipantGroupAddress pgAddress = 
    							new ParticipantGroupAddress(null, null, multicastIPAddress, null);
    					
    					if (groupRecord.getRecordType() == 
    							IGMPv3GroupRecord.RECORD_TYPE_CHANGE_TO_EXCLUDE_MODE) { // Join Group
    						// Add Participant
    						multicastingService.addParticipant(pgAddress, srcIntf, ap);
    					}
    					else if (groupRecord.getRecordType() == 
    							IGMPv3GroupRecord.RECORD_TYPE_CHANGE_TO_INCLUDE_MODE) { // Leave Group
    						// Remove Participant
    						multicastingService.removeParticipant(pgAddress, srcIntf, ap);
    					}
    				}
    			}
        	}
        	else {
        		if (destIPAddress != null && 
        				destIPAddress.isMulticast()) {
    				IRoutingDecision decision = new RoutingDecision(sw.getId(), 
    						OFMessageUtils.getInPort(msg), 
                    		IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE),
                            IRoutingDecision.RoutingAction.MULTICAST);
    				ParticipantGroupAddress pgAddress = 
							new ParticipantGroupAddress(null, null, destIPAddress, null);
    				decision.setParticipantGroupAddress(pgAddress);
    				decision.addToContext(cntx);
        		}
        	}
        }
		return Command.CONTINUE;
	}
	
	@Override
	public String getName() {
		return "igmpmanager";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && 
				(name.equals("linkdiscovery") || 
						name.equals("topology") || 
						name.equals("devicemanager")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && 
				(name.equals("forwarding")));
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType())
		{
			case PACKET_IN:
				return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
			default:
				return Command.CONTINUE;
		}
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    l.add(IMulticastService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		multicastingService = context.getServiceImpl(IMulticastService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		multicastingService.addListener(this);
	}

	@Override
	public void ParticipantAdded(ParticipantGroupAddress groupAddress, MacVlanPair intf, NodePortTuple ap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ParticipantRemoved(ParticipantGroupAddress groupAddress, MacVlanPair intf, NodePortTuple ap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ParticipantsReset() {
		// TODO Auto-generated method stub
		
	}
}
