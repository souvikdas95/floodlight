package net.floodlightcontroller.simpleigmp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.core.types.MacVlanPair;

// Bi-directional Map Table to store IGMP Group Membership Information
public class IGMPTable
{
	private Map<IPv4Address, Set<MacVlanPair>> mcastToMacVlanMap;
	private Map<MacVlanPair, Set<IPv4Address>> macVlanToMcastMap;
	
	public IGMPTable()
	{
		mcastToMacVlanMap = new ConcurrentHashMap<IPv4Address, Set<MacVlanPair>>();
		macVlanToMcastMap = new ConcurrentHashMap<MacVlanPair, Set<IPv4Address>>();
	}
	
	public void add(IPv4Address mcastAddress, MacAddress macAddress, VlanVid vlan)
	{
		MacVlanPair macVlanPair = new MacVlanPair(macAddress, vlan);
		
		// Add to Member Set
		if (!mcastToMacVlanMap.containsKey(mcastAddress))
		{
			mcastToMacVlanMap.put(mcastAddress, new HashSet<MacVlanPair>());
		}
		Set<MacVlanPair> memberSet = mcastToMacVlanMap.get(mcastAddress);
		if (!memberSet.contains(macVlanPair))
		{
			memberSet.add(macVlanPair);
		}
		
		// Add to Group Set
		if (!macVlanToMcastMap.containsKey(macVlanPair))
		{
			macVlanToMcastMap.put(macVlanPair, new HashSet<IPv4Address>());
		}
		Set<IPv4Address> groupSet = macVlanToMcastMap.get(macVlanPair);
		if (!groupSet.contains(mcastAddress))
		{
			groupSet.add(mcastAddress);
		}
	}
	
	public void remove(IPv4Address mcastAddress, MacAddress macAddress, VlanVid vlan)
	{
		MacVlanPair macVlanPair = new MacVlanPair(macAddress, vlan);
		
		// Remove from Member Set
		if (mcastToMacVlanMap.containsKey(mcastAddress))
		{
			Set<MacVlanPair> memberSet = mcastToMacVlanMap.get(mcastAddress);
			if (memberSet.contains(macVlanPair))
			{
				memberSet.remove(macVlanPair);
				if (memberSet.isEmpty())
				{
					mcastToMacVlanMap.remove(mcastAddress);
				}
			}
		}
		
		// Remove from Group Set
		if (macVlanToMcastMap.containsKey(macVlanPair))
		{
			Set<IPv4Address> groupSet = macVlanToMcastMap.get(macVlanPair);
			if (groupSet.contains(mcastAddress))
			{
				groupSet.remove(mcastAddress);
				if (groupSet.isEmpty())
				{
					macVlanToMcastMap.remove(macVlanPair);
				}
			}
		}
	}

	public boolean has(IPv4Address mcastAddress, MacAddress macAddress, VlanVid vlan)
	{
		MacVlanPair macVlanPair = new MacVlanPair(macAddress, vlan);
		
		if (mcastToMacVlanMap.containsKey(mcastAddress) && macVlanToMcastMap.containsKey(macVlanPair))
		{
			return true;
		}
		
		return false;
	}
	
	public Set<MacVlanPair> getMembers(IPv4Address mcastAddress)
	{
		if (mcastToMacVlanMap.containsKey(mcastAddress))
		{
			return new HashSet<MacVlanPair>(mcastToMacVlanMap.get(mcastAddress));
		}
		
		return null;
	}
	
	public Set<IPv4Address> getGroups(MacAddress macAddress, VlanVid vlan)
	{
		MacVlanPair macVlanPair = new MacVlanPair(macAddress, vlan);
		
		if (macVlanToMcastMap.containsKey(macVlanPair))
		{
			return new HashSet<IPv4Address>(macVlanToMcastMap.get(macVlanPair));
		}
		
		return null;
	}
	
	public boolean isMember(MacAddress macAddress, VlanVid vlan)
	{
		MacVlanPair macVlanPair = new MacVlanPair(macAddress, vlan);
		
		return macVlanToMcastMap.containsKey(macVlanPair);
	}
	
	public boolean isGroup(IPv4Address mcastAddress)
	{
		return mcastToMacVlanMap.containsKey(mcastAddress);
	}
	
	public void deleteGroup(IPv4Address mcastAddress)
	{
		if (mcastToMacVlanMap.containsKey(mcastAddress))
		{
			Set<MacVlanPair> memberSet = mcastToMacVlanMap.get(mcastAddress);
			
			for(MacVlanPair macVlanPair: memberSet)
			{
				remove(mcastAddress, macVlanPair.getMac(), macVlanPair.getVlan());
			}
		}
	}
	
	public void deleteMember(MacAddress macAddress, VlanVid vlan)
	{
		MacVlanPair macVlanPair = new MacVlanPair(macAddress, vlan);
		
		if (macVlanToMcastMap.containsKey(macVlanPair))
		{
			Set<IPv4Address> groupSet = macVlanToMcastMap.get(macVlanPair);
			
			for(IPv4Address mcastAddress: groupSet)
			{
				remove(mcastAddress, macVlanPair.getMac(), macVlanPair.getVlan());
			}
		}
	}
	
	public void clearTable()
	{
		mcastToMacVlanMap.clear();
		macVlanToMcastMap.clear();
	}
}
