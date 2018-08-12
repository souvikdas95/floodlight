package net.floodlightcontroller.multicasting.igmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.slf4j.Logger;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.multicasting.IMulticastListener;
import net.floodlightcontroller.multicasting.IMulticastService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IGMP;
import net.floodlightcontroller.packet.IGMP.IGMPv3GroupRecord;
import net.floodlightcontroller.packet.IPv4;

public class IGMPManager implements IFloodlightModule, IOFMessageListener, IMulticastListener {
	
	protected static Logger logger;
	
	protected IFloodlightProviderService floodlightProvider;

	protected IMulticastService multicastingService;
	
	// Processes PacketIn Message
	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx) {
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
        
		if (ethType.equals(EthType.IPv4)) {
        	IPv4 ipv4 = (IPv4) eth.getPayload();
        	IpProtocol ipProtocol = ipv4.getProtocol();
        	
        	if (ipProtocol.equals(IpProtocol.IGMP)) { 		
        		IGMP igmp = (IGMP) ipv4.getPayload();
        		IDevice srcDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);
        		
        		if (igmp.isIGMPv3MembershipReportMessage()) {
    				IGMPv3GroupRecord[] groupRecords = igmp.getGroupRecords();
    				
    				for (IGMPv3GroupRecord groupRecord: groupRecords) {
    					IPv4Address mcastAddress = groupRecord.getMulticastAddress();
    					
    					if (groupRecord.getRecordType() == 
    							IGMPv3GroupRecord.RECORD_TYPE_CHANGE_TO_EXCLUDE_MODE) { // Join Group
    						// Add Participant
    						multicastingService.addParticipant(mcastAddress, srcDevice);
    					}
    					else if (groupRecord.getRecordType() == 
    							IGMPv3GroupRecord.RECORD_TYPE_CHANGE_TO_INCLUDE_MODE) { // Leave Group
    						// Remove Participant
    						multicastingService.removeParticipant(mcastAddress, srcDevice);
    					}
    				}
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
		return (type.equals(OFType.PACKET_IN) && (name.equals("topology")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
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
	public void ParticipantAdded(IPAddress<?> mcastAddress, IDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ParticipantRemoved(IPAddress<?> mcastAddress, IDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ParticipantGroupRemoved(IPAddress<?> mcastAddress) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ParticipantMemberRemoved(IDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ParticipantsReset() {
		// TODO Auto-generated method stub

	}
}
