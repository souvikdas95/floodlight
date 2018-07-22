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
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IPv6;
import net.floodlightcontroller.simpleigmp.IGMP.IGMPv3GroupRecord;
import net.floodlightcontroller.util.FlowModUtils;
import net.floodlightcontroller.util.OFMessageUtils;

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
    					}
    					else if (groupRecord.getRecordType() == groupRecord.RECORD_TYPE_CHANGE_TO_INCLUDE_MODE) // Leave Group
    					{
    						// Remove Multicast Group Member
    						if (igmpTable.has(mcastAddress, sourceMACAddress, vlan))
    							igmpTable.remove(mcastAddress, sourceMACAddress, vlan);
    						
    						// Remove MAC Table Entries if no longer
    						// part of any Group
    						if (!igmpTable.isMember(sourceMACAddress, vlan))
    							macTable.removeFromPortMap(sw, sourceMACAddress, vlan);
    					}
    					
    					// Invalidate all existing flow mods belonging to this mcast group
    	        		Match.Builder matchBuilder = sw.getOFFactory().buildMatch();
    	        		matchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv4)
    	        					.setExact(MatchField.IPV4_DST, mcastAddress);
    	        		Match match = matchBuilder.build();
    	        		this.writeFlowMod(sw, OFFlowModCommand.DELETE, OFBufferId.NO_BUFFER, match, null);
    				}
    			}
        	}
        	else
        	{
        		IPv4Address destIPAddress = ipv4.getDestinationAddress();
        		if (destIPAddress == null || !destIPAddress.isMulticast() || !igmpTable.isGroup(destIPAddress))
        		{
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
        		
        		// Install Flow
        		this.writeFlowMod(sw, OFFlowModCommand.ADD, OFBufferId.NO_BUFFER, match, outPorts);
        		
        		return Command.CONTINUE;
        	}
        }
		
		return Command.CONTINUE;
	}

	private void writeFlowMod(IOFSwitch sw, OFFlowModCommand command, OFBufferId bufferId, Match match, Set<OFPort> outPorts)
	{
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
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		if (command != OFFlowModCommand.DELETE)
		{
			sfmf.add(OFFlowModFlags.SEND_FLOW_REM);
		}
		fmb.setFlags(sfmf);

		if (outPorts != null && !outPorts.isEmpty())
		{
			List<OFAction> actions = new ArrayList<OFAction>();
			for(OFPort outPort: outPorts)
			{
				actions.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(0xffFFffFF).build());
			}
			FlowModUtils.setActions(fmb, actions, sw);
		}

		sw.write(fmb.build());
	}
	
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType())
		{
			case PACKET_IN:
				return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
			case ERROR:
				return Command.CONTINUE;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name)
	{
		// TODO Auto-generated method stub
		return false;
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
	    
	    logger.info("Init");
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
	{
		logger.info("StartUp");
		
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
		floodlightProvider.addOFMessageListener(OFType.ERROR, this);
		
		Map<String, String> configOptions = context.getConfigParams(this);
		
		// Get FLOWMOD_PRIORITY
		try
		{
			String priority = configOptions.get("priority");
			if (priority != null)
			{
				FLOWMOD_PRIORITY = Short.parseShort(priority);
			}
		} 
		catch (NumberFormatException e)
		{
			logger.warn("Error parsing flow priority, " +
					"using default of {}",
					FLOWMOD_PRIORITY);
		}
	}

}
