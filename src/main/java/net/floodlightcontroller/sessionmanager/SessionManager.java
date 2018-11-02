package net.floodlightcontroller.sessionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.projectfloodlight.openflow.types.TransportPort;
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
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.multicasting.IMulticastListener;
import net.floodlightcontroller.multicasting.IMulticastService;
import net.floodlightcontroller.multicasting.internal.ParticipantGroupAddress;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.util.OFMessageUtils;
import net.sourceforge.jsdp.SessionDescription;

public class SessionManager implements IOFMessageListener, IFloodlightModule, IMulticastListener
{
	protected static Logger logger;
	
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApiService;
	
	protected IMulticastService multicastingService;
	
	@Override
	public String getName()
	{
		return "sessionmanager";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name)
	{
		return (type.equals(OFType.PACKET_IN) &&
				(name.equals("linkdiscovery") || 
						name.equals("topology") || 
						name.equals("devicemanager")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name)
	{
		return (type.equals(OFType.PACKET_IN) && 
				name.equals("forwarding"));
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices()
	{
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
	{
		Map<Class<? extends IFloodlightService>,  IFloodlightService> m = 
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
	{
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		l.add(IMulticastService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException
	{
		logger = LoggerFactory.getLogger(SessionManager.class);
		floodlightProvider = (IFloodlightProviderService) context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = (IRestApiService) context.getServiceImpl(IRestApiService.class);
		multicastingService = (IMulticastService) context.getServiceImpl(IMulticastService.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiService.addRestletRoutable(new SessionManagerRoutable());
		multicastingService.addListener(this);
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx)
	{
		switch (msg.getType())
		{
			case PACKET_IN:
				return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
			default:
				return Command.CONTINUE;
		}
	}

	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx)
	{
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
        
		if (ethType == EthType.IPv4) {
			IPv4 ipv4 = (IPv4) eth.getPayload();
			IPv4Address destIPAddress = ipv4.getDestinationAddress();
			if (destIPAddress == null ||
					destIPAddress == IPv4Address.NONE) {
				return Command.CONTINUE;
			}
			if (destIPAddress.isMulticast()) {
				if (ipv4.getProtocol() == IpProtocol.UDP) {
					UDP udp = (UDP) ipv4.getPayload();
    				IRoutingDecision decision = new RoutingDecision(sw.getId(), 
    						inPort, 
                    		IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE),
                            IRoutingDecision.RoutingAction.MULTICAST);
    				ParticipantGroupAddress pgAddress = 
							new ParticipantGroupAddress(null, null, destIPAddress, udp.getDestinationPort());
    				decision.setParticipantGroupAddress(pgAddress);
    				decision.addToContext(cntx);
				}
				return Command.CONTINUE;
			}
			if (ipv4.getProtocol() == IpProtocol.UDP) {
				UDP udp = (UDP) ipv4.getPayload();
				
				try {
					SAP sap = new SAP(udp.getPayload());	// Exception if not valid Payload
					SDP sdp = new SDP(sap.getPayload());	// Exception if not valid Payload
					SessionDescription sessionDescription = sdp.getSessionDescription();
					
					// Destination in Reply SAP is Participant
					MacVlanPair participantIntf = new MacVlanPair(destMACAddress, vlanVid);
					
					// When the Reply SAP is at Participant's Attachment Point
					IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
					NodePortTuple participantAp = null;
					SwitchPort sp = dstDevice.getAttachmentPoints()[0];
					if (sp == null) {
						logger.error("No AP found for participant!");
						return Command.CONTINUE;
					}
					participantAp = new NodePortTuple(sp.getNodeId(), sp.getPortId());
	        		
					// Get Stream IP Address & Port
					IPv4Address streamAddress = IPv4Address.of(sessionDescription.getConnection()
							.getAddress().split("/", 2)[0]);
					if (streamAddress == null || !streamAddress.isMulticast()) {
						return Command.CONTINUE;
					}
					TransportPort streamPort = TransportPort.of(
							sessionDescription.getMediaDescriptions()[0].getMedia().getPort());
					ParticipantGroupAddress pgAddress = new ParticipantGroupAddress(null, null, streamAddress, streamPort);
					
					logger.info(String.format("(%s), %s, %s", pgAddress, participantIntf, participantAp));
					multicastingService.addParticipant(pgAddress, participantIntf, participantAp);
				}
				catch (Exception ex) {
					return Command.CONTINUE;
				}
			}
		}
		return Command.CONTINUE;
	}

	@Override
	public void ParticipantsReset() {
		// nothing to do
	}
	
	@Override
	public void ParticipantAdded(ParticipantGroupAddress group, MacVlanPair intf, NodePortTuple ap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ParticipantRemoved(ParticipantGroupAddress group, MacVlanPair intf, NodePortTuple ap) {
		// TODO Auto-generated method stub
		
	}
}
