package net.floodlightcontroller.simpleigmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
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
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.simpleigmp.IGMP.IGMPv3GroupRecord;
import net.floodlightcontroller.util.FlowModUtils;

public class SimpleIGMP implements IFloodlightModule, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	
	// For a given switch, this maps destination IGMP member's MacVlan (unicast) to Switch Port
	// Note: This is only to be used for reference and needs to be persistent as long as MCast group is valid.
	protected MACTable macTable;
	
	// For a given switch, this maps destination IGMP group IP to list of Switch Ports
	// Note: This directly corresponds to installed flow rules.
	protected IGMPTable igmpTable;
	
	// Fixed Params
	public static final int SIMPLE_IGMP_APP_ID = 1024;
	public static final int APP_ID_BITS = 12;
	public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
	public static final long SIMPLE_IGMP_COOKIE = (long) (SIMPLE_IGMP_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
	
	// Config Params
	protected static boolean DEBUG = false;
	protected static short FLOWMOD_PRIORITY = 200;

	// Processes PacketIn Message
	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx)
	{		
		OFPort inPort = (msg.getVersion().compareTo(OFVersion.OF_12) < 0 ? msg.getInPort() : msg.getMatch().get(MatchField.IN_PORT));
		
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		EthType ethType = eth.getEtherType();
		MacAddress sourceMACAddress = eth.getSourceMACAddress();
		MacAddress destMACAddress = eth.getDestinationMACAddress();
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		
		if (sourceMACAddress == null)
		{
			sourceMACAddress = MacAddress.NONE;
		}
		
		if (destMACAddress == null)
		{
			destMACAddress = MacAddress.NONE;
		}
		
		if (vlan == null)
		{
			vlan = VlanVid.ZERO;
		}

		// Ignoring 802.1Q/D packets
		if ((destMACAddress.getLong() & 0xfffffffffff0L) == 0x0180c2000000L)
		{
			return Command.CONTINUE;
		}
		
        if (ethType == EthType.IPv4)
        {     	
        	IPv4 ipv4 = (IPv4) eth.getPayload();
        	IpProtocol ipProtocol = ipv4.getProtocol();
        	
        	if (ipProtocol == ipProtocol.IGMP)
        	{
        		if (sourceMACAddress == MacAddress.NONE)
        		{
        			return Command.CONTINUE;
        		}
        		
        		IGMP igmp = (IGMP) ipv4.getPayload();
    			
        		if (igmp.isIGMPv3MembershipReportMessage())
    			{
    				IGMPv3GroupRecord[] groupRecords = igmp.getGroupRecords();
    				for (IGMPv3GroupRecord groupRecord: groupRecords)
    				{
    					IPv4Address mcastAddress = groupRecord.getMulticastAddress();
    					
    					if (groupRecord.getRecordType() == groupRecord.RECORD_TYPE_CHANGE_TO_EXCLUDE_MODE) // Join Group
    					{
    						if (DEBUG)
    							logger.info(String.format("PACKET_IN: [IGMP:JOIN] [Source:'%s'] [Group:'%s'] [Switch:'%s']", ipv4.getSourceAddress(), mcastAddress, sw));
    						
    						// Add MAC Table Entries
    		        		// Note: It's expected that the membership reports will
    		        		// reach each and every switch. So, it's just enough for
    		        		// us to populate a reference MACTable that can be used
    		        		// to determine OFPort(s) (each leading to a member) that 
    		        		// will be used to forward mcast packets at a given switch.
    						macTable.addToPortMap(sw, sourceMACAddress, vlan, inPort);
    		        		
    		        		// Add Multicast Group Member
    		        		if (!igmpTable.has(mcastAddress, sourceMACAddress, vlan))
    		        			igmpTable.add(mcastAddress, sourceMACAddress, vlan);
    		        		
        					// Invalidate all existing flow mods belonging to this mcast group
    						// to allow re-establishment of routes
        	        		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
        	        		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
        	        					.setExact(MatchField.IPV4_DST, mcastAddress);
        	        		Match match = matchBuilder.build();
        	        		this.writeFlowMod(sw, OFFlowModCommand.DELETE, OFBufferId.NO_BUFFER, match, null);
    					}
    					else if (groupRecord.getRecordType() == groupRecord.RECORD_TYPE_CHANGE_TO_INCLUDE_MODE) // Leave Group
    					{
    						if (DEBUG)
    							logger.info(String.format("PACKET_IN: [IGMP:LEAVE] [Source:'%s'] [Group:'%s'] [Switch:'%s']", ipv4.getSourceAddress(), mcastAddress, sw));    						
    						
    						// Remove Multicast Group Member
    						if (igmpTable.has(mcastAddress, sourceMACAddress, vlan))
    							igmpTable.remove(mcastAddress, sourceMACAddress, vlan);
    						
    						// Remove MAC Table Entries if no longer
    						// part of any Group
    						if (!igmpTable.isMember(sourceMACAddress, vlan))
    							macTable.removeFromPortMap(sw, sourceMACAddress, vlan);
    						
        					// Invalidate all existing flow mods belonging to this mcast group
    						// to allow re-establishment of routes
        	        		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
        	        		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
        	        					.setExact(MatchField.IPV4_DST, mcastAddress);
        	        		Match match = matchBuilder.build();
        	        		this.writeFlowMod(sw, OFFlowModCommand.DELETE, OFBufferId.NO_BUFFER, match, null);
    					}
    				}
    			}
        	}
        	else
        	{
        		// Validate Destination IP for Multicast
        		IPv4Address destIPAddress = ipv4.getDestinationAddress();
        		if (destIPAddress == null || !destIPAddress.isMulticast())
        		{
        			return Command.CONTINUE;
        		}
        		
        		// Check if address belongs to IGMP Table
        		if (!igmpTable.isGroup(destIPAddress))
        		{
        			if (DEBUG)
        				logger.info(String.format("PACKET_IN: Mcast Address '%s' not known to Switch '%s'", destIPAddress, sw));
        			
        			return Command.CONTINUE;
        		}
        		
        		// Set of IGMP Group Members
        		Set<MacVlanPair> members = igmpTable.getMembers(destIPAddress);
        		
        		// Set of output Ports
        		Set<OFPort> outPorts = new HashSet<OFPort>();
        		for(MacVlanPair member: members)
        		{
        			OFPort outPort = macTable.getFromPortMap(sw, member.getMac(), member.getVlan());

        			if (outPort == null)
        			{
        				return Command.CONTINUE;
        			}
        			
        			if (outPort == inPort)
        			{
        				continue;
        			}
        			
        			if (!outPorts.contains(outPort))
        				outPorts.add(outPort);
        		}
        		
        		// Prepare Match
        		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
        		matchBuilder.setExact(MatchField.IN_PORT, inPort)
        					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
        					.setExact(MatchField.IPV4_DST, destIPAddress);
        		Match match = matchBuilder.build();
        		
        		// Push Packet manually
        		// Note: If outPorts is empty, then this packet will be dropped.
        		// Note: Make sure to return Command.STOP
        		// this.pushPacket(sw, match, msg, outPorts);
        		
        		// Install Flow
        		// Note: If outPorts is empty, then packets will be dropped by this flow rule.
        		this.writeFlowMod(sw, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, match, outPorts);
        		
        		// Log Flowmod Add
        		if (DEBUG)
        		{
        			StringBuilder sbOut = new StringBuilder();
	        		for (OFPort outPort: outPorts)
	        		{
	        			sbOut.append("'" + outPort + "'");
	        			sbOut.append(",");
	        		}
	        		logger.info(String.format("PACKET_IN: [FLOWMOD:ADD] [SWITCH:'%s'] [IN_PORT:'%s'] [IPV4_DST:'%s'] [OUTPORTS:{%s}]", sw, inPort, destIPAddress, sbOut));
        		}
        	}
        }
		
		return Command.CONTINUE;
	}

	/*private void pushPacket(IOFSwitch sw, Match match, OFPacketIn msg, Set<OFPort> outPorts)
	{
		if (msg == null)
		{
			return;
		}
		
		// Create PacketOut
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();

		// Set Actions
		if (outPorts != null && !outPorts.isEmpty())
		{
			List<OFAction> actions = new ArrayList<OFAction>();
			for(OFPort outPort: outPorts)
			{
				actions.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(0xffFFffFF).build());
			}
			pob.setActions(actions);
		}
		
		// Set BufferId (If switch supports it)
		if (sw.getBuffers() == 0)
		{
			msg = msg.createBuilder().setBufferId(OFBufferId.NO_BUFFER).build();
			pob.setBufferId(OFBufferId.NO_BUFFER);
		}
		else
		{
			pob.setBufferId(msg.getBufferId());
		}

		// Set Input Port
		OFPort inPort = (msg.getVersion().compareTo(OFVersion.OF_12) < 0 ? msg.getInPort() : msg.getMatch().get(MatchField.IN_PORT));
		OFMessageUtils.setInPort(pob, inPort);

		// If BufferId is none or the switch doesn's support buffering
		// We send the data with the PacketOut
		if (msg.getBufferId() == OFBufferId.NO_BUFFER)
		{
			byte[] packetData = msg.getData();
			pob.setData(packetData);
		}

		// Write to switch
		sw.write(pob.build());
	}*/
	
	private void writeFlowMod(IOFSwitch sw, OFFlowModCommand command, OFBufferId bufferId, Match match, Set<OFPort> outPorts)
	{
		// Create Flowmod Builder
		OFFlowMod.Builder fmb;
		if (command == OFFlowModCommand.DELETE)
		{
			fmb = sw.getOFFactory().buildFlowDelete();
		}
		else
		{
			fmb = sw.getOFFactory().buildFlowAdd();
		}
		fmb.setMatch(match);
		fmb.setCookie((U64.of(SimpleIGMP.SIMPLE_IGMP_COOKIE)));
		fmb.setIdleTimeout(0);	// Infinite
		fmb.setHardTimeout(0);	// Infinite
		fmb.setPriority(SimpleIGMP.FLOWMOD_PRIORITY);
		fmb.setBufferId(bufferId);
		if (command == OFFlowModCommand.DELETE)
		{
			fmb.setOutPort(OFPort.ANY);
		}
		
		// Create FlowModFlags
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		if (command != OFFlowModCommand.DELETE)
		{
			sfmf.add(OFFlowModFlags.SEND_FLOW_REM);
		}
		fmb.setFlags(sfmf);

		// Set Actions
		if (outPorts != null && !outPorts.isEmpty())
		{
			List<OFAction> actions = new ArrayList<OFAction>();
			for(OFPort outPort: outPorts)
			{
				actions.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(0xffFFffFF).build());
			}
			FlowModUtils.setActions(fmb, actions, sw);
		}

		// Write to switch
		sw.write(fmb.build());
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
	public String getName()
	{
		return SimpleIGMP.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name)
	{
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name)
	{
		return (type.equals(OFType.PACKET_IN) && (name.equals("learningswitch")));
	}
	

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
	{
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		macTable = new MACTable();
		igmpTable = new IGMPTable();
	    logger = LoggerFactory.getLogger(SimpleIGMP.class);
	    
	    // Patch IPv4 protocolClassMap to support IGMP
	    if (!IPv4.protocolClassMap.containsKey(IpProtocol.IGMP))
	    {
	    	IPv4.protocolClassMap.put(IpProtocol.IGMP, IGMP.class);
	    }
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		Map<String, String> configOptions = context.getConfigParams(this);
		
		// Get DEBUG
		try
		{
			String debug = configOptions.get("debug");
			if (debug != null)
			{
				DEBUG = (debug.charAt(0) == 'T' || debug.charAt(0) == 't') ? true : false;
			}
		} 
		catch (NumberFormatException e)
		{
			logger.warn("Error parsing 'debug', " +
					"using default of {}",
					DEBUG);
		}
		
		// Get FLOWMOD_PRIORITY
		try
		{
			String flow_priority = configOptions.get("flow_priority");
			if (flow_priority != null)
			{
				FLOWMOD_PRIORITY = Short.parseShort(flow_priority);
			}
		} 
		catch (NumberFormatException e)
		{
			logger.warn("Error parsing 'flow_priority', " +
					"using default of {}",
					FLOWMOD_PRIORITY);
		}
		
		if (DEBUG)
			logger.info("StartUp: SimpleIGMP Module");
	}

}
