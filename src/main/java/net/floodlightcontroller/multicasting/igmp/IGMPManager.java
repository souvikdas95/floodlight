package net.floodlightcontroller.multicasting.igmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
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
import net.floodlightcontroller.multicasting.internal.ParticipantGroupOptions;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IGMP;
import net.floodlightcontroller.packet.IGMP.IGMPv3GroupRecord;
import net.floodlightcontroller.util.OFMessageUtils;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.IRoutingDecision;
import net.floodlightcontroller.routing.RoutingDecision;
import net.floodlightcontroller.topology.ITopologyService;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Manager for IGMP based Multicasting
 * 
 */ 
public class IGMPManager implements IFloodlightModule, IOFMessageListener, IMulticastListener {
    
    protected static Logger logger = LoggerFactory.getLogger(IGMPManager.class);
    
    protected static Integer DEFAULT_FLOW_PRIORITY = 1;
    protected static TableId DEFAULT_TABLE_ID = TableId.ZERO;
    protected static Integer DEFAULT_QUEUE_ID = null;
    protected static Integer DEFAULT_IDLE_TIMEOUT = 5;
    protected static Integer DEFAULT_HARD_TIMEOUT = 0;
    
    protected IFloodlightProviderService floodlightProvider;
    protected ITopologyService topologyService;
    protected IMulticastService multicastService;
    
    // Processes PacketIn Message
    private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx) {
        // Invalid Packet
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        MacAddress sourceMACAddress = eth.getSourceMACAddress();
        if (sourceMACAddress == null) {
            return Command.CONTINUE;
        }
        
        // Ignoring 802.1Q/D packets
        MacAddress destMACAddress = eth.getDestinationMACAddress();
        if (destMACAddress == null) {
            destMACAddress = MacAddress.NONE;
        }
        if ((destMACAddress.getLong() & 0xfffffffffff0L) == 0x0180c2000000L) {
            return Command.CONTINUE;
        }
        
        // Only process packets received at AP
        OFPort inPort = OFMessageUtils.getInPort(msg);
        DatapathId swId = sw.getId();
        if (!topologyService.isAttachmentPointPort(swId, inPort)) {
            return Command.CONTINUE;
        }
        
        EthType ethType = eth.getEtherType();
        if (ethType.equals(EthType.IPv4)) {
            // VLAN ID
            VlanVid vlanVid = null; 
            if (msg.getVersion().compareTo(OFVersion.OF_11) > 0 && /* 1.0 and 1.1 do not have a match */
                    msg.getMatch().get(MatchField.VLAN_VID) != null) { 
                vlanVid = msg.getMatch().get(MatchField.VLAN_VID).getVlanVid(); /* VLAN may have been popped by switch */
            }
            if (vlanVid == null) {
                vlanVid = VlanVid.ofVlan(eth.getVlanID()); /* VLAN might still be in packet */
            }
            
            // Process IGMP Packets
            IPv4 ipv4 = (IPv4) eth.getPayload();
            IpProtocol ipProtocol = ipv4.getProtocol();
            if (ipProtocol.equals(IpProtocol.IGMP)) {
                // Check for IGMPv3 Membership Report Messages only
                IGMP igmp = (IGMP) ipv4.getPayload();
                if (!igmp.isIGMPv3MembershipReportMessage()) {
                    return Command.CONTINUE;
                }
                
                // Participant Interface
                MacVlanPair srcIntf = new MacVlanPair(sourceMACAddress, vlanVid);
                
                // Participant AP
                NodePortTuple srcAp = new NodePortTuple(swId, inPort);
                
                // Process Group Records
                IGMPv3GroupRecord[] groupRecords = igmp.getGroupRecords();
                for (IGMPv3GroupRecord groupRecord: groupRecords) {
                    // Participant Group Address
                    ParticipantGroupAddress groupAddress = 
                            new ParticipantGroupAddress(null, vlanVid, 
                                    groupRecord.getMulticastAddress(), null);
                    
                    // Participant Group Options
                    ParticipantGroupOptions pgOpts = multicastService.getParticipantGroupOptions(groupAddress);
                    if (pgOpts == null) {
                        pgOpts = new ParticipantGroupOptions(groupAddress);
                        multicastService.setParticipantGroupOptions(groupAddress, pgOpts);
                    }
                    pgOpts.setFlowPriority(DEFAULT_FLOW_PRIORITY);
                    pgOpts.setTableId(DEFAULT_TABLE_ID);
                    pgOpts.setQueueId(DEFAULT_QUEUE_ID);
                    pgOpts.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
                    pgOpts.setHardTimeout(DEFAULT_HARD_TIMEOUT);
                    
                    if (groupRecord.getRecordType() == 
                            IGMPv3GroupRecord.RECORD_TYPE_CHANGE_TO_EXCLUDE_MODE) {
                        // Join Group
                        multicastService.addParticipant(groupAddress, srcIntf, srcAp);
                    }
                    else if (groupRecord.getRecordType() == 
                            IGMPv3GroupRecord.RECORD_TYPE_CHANGE_TO_INCLUDE_MODE) {
                        // Leave Group
                        multicastService.removeParticipant(groupAddress, srcIntf, srcAp);
                    }
                }
            }
            else {    // Process Multicast Packets
                IPv4Address destIPAddress = ipv4.getDestinationAddress();
                if (!destIPAddress.isMulticast()) {
                    return Command.CONTINUE;
                }
                
                // Participant Group Address
                ParticipantGroupAddress groupAddress = 
                        new ParticipantGroupAddress(null, vlanVid, destIPAddress, null);
                
                // Check if Participant Group exists
                if (!multicastService.hasParticipantGroupAddress(groupAddress)) {
                    // return Command.STOP to drop instead of flooding packet by default.
                    return Command.CONTINUE;
                }
                
                // Set Forwarding Decision
                IRoutingDecision decision = new RoutingDecision(sw.getId(), 
                        inPort, 
                        IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE),
                        IRoutingDecision.RoutingAction.MULTICAST);
                decision.setParticipantGroupAddress(groupAddress);
                decision.addToContext(cntx);
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
                name.equals("forwarding"));
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
        l.add(ITopologyService.class);
        l.add(IMulticastService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        multicastService = context.getServiceImpl(IMulticastService.class);
        topologyService = context.getServiceImpl(ITopologyService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        multicastService.addListener(this);
    }

    @Override
    public void ParticipantsReset() {
        // TODO Auto-generated method stub
        logger.info("All Participants Reset");
    }

    @Override
    public void ParticipantAdded(ParticipantGroupAddress group, MacVlanPair intf, NodePortTuple ap) {
        // TODO Auto-generated method stub
        logger.info("Participant Added: \n" + group + "\n" + intf + "\n" + ap);
    }

    @Override
    public void ParticipantRemoved(ParticipantGroupAddress group, MacVlanPair intf, NodePortTuple ap) {
        // TODO Auto-generated method stub
        logger.info("Participant Removed: \n" + group + "\n" + intf + "\n" + ap);
    }
}
