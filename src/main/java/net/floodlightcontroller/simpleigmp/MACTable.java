package net.floodlightcontroller.simpleigmp;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.projectfloodlight.openflow.util.LRULinkedHashMap;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.types.MacVlanPair;

public class MACTable
{
	private Map<IOFSwitch, Map<MacVlanPair, OFPort>> macVlanToSwitchPortMap;
	
	public MACTable()
	{
		macVlanToSwitchPortMap = new ConcurrentHashMap<IOFSwitch, Map<MacVlanPair, OFPort>>();
	}
	
	private static final int MAX_MACS_PER_SWITCH = 1000;
	
	protected void addToPortMap(IOFSwitch sw, MacAddress mac, VlanVid vlan, OFPort portVal)
	{
		Map<MacVlanPair, OFPort> swMap = macVlanToSwitchPortMap.get(sw);

		if (vlan == VlanVid.FULL_MASK || vlan == null)
		{
			vlan = VlanVid.ofVlan(0);
		}

		if (swMap == null)
		{
			swMap = Collections.synchronizedMap(new LRULinkedHashMap<MacVlanPair, OFPort>(MAX_MACS_PER_SWITCH));
			macVlanToSwitchPortMap.put(sw, swMap);
		}
		
		swMap.put(new MacVlanPair(mac, vlan), portVal);
	}

	protected void removeFromPortMap(IOFSwitch sw, MacAddress mac, VlanVid vlan)
	{
		if (vlan == VlanVid.FULL_MASK)
		{
			vlan = VlanVid.ofVlan(0);
		}

		Map<MacVlanPair, OFPort> swMap = macVlanToSwitchPortMap.get(sw);
		if (swMap != null)
		{
			swMap.remove(new MacVlanPair(mac, vlan));
		}
	}

	public OFPort getFromPortMap(IOFSwitch sw, MacAddress mac, VlanVid vlan)
	{
		if (vlan == VlanVid.FULL_MASK || vlan == null)
		{
			vlan = VlanVid.ofVlan(0);
		}
		
		Map<MacVlanPair, OFPort> swMap = macVlanToSwitchPortMap.get(sw);
		
		if (swMap != null)
		{
			return swMap.get(new MacVlanPair(mac, vlan));
		}

		return null;
	}

	public void clearTable()
	{
		macVlanToSwitchPortMap.clear();
	}

	public void clearTable(IOFSwitch sw)
	{
		Map<MacVlanPair, OFPort> swMap = macVlanToSwitchPortMap.get(sw);
		
		if (swMap != null)
		{
			swMap.clear();
		}
	}
}
