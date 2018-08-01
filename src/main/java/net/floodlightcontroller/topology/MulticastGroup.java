package net.floodlightcontroller.topology;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.routing.BroadcastTree;

/*
 * Must exist per archipelago
 */
public class MulticastGroup {
	
	// Multicast Group Id
	private DatapathId id;
	
	// Parent Archipelago (only used for reference & hashCode)
	private final Archipelago archipelago;
	
	// Set of Devices that belong to this Multicast Group
	private Set<IDevice> devices;
	
	// Destiantions Rooted Multicast Trees
	private Map<DatapathId, BroadcastTree> destinationsRootedMulticastTrees;
	
	public MulticastGroup(Archipelago archipelago) {
		id = DatapathId.NONE;
		this.archipelago = archipelago;
		devices = new HashSet<IDevice>();
		destinationsRootedMulticastTrees = new HashMap<DatapathId, BroadcastTree>();
	}
	
	public DatapathId getId() {
		return id;
	}
	
	public void setId(DatapathId id) {
		this.id = id;
	}
	
	public Archipelago getArchiepelago() {
		return archipelago;
	}
	
	public void addDevice(IDevice device) {
		devices.add(device);
	}
	
	public void removeDevice(IDevice device) {
		devices.remove(device);
	}
	
	public boolean hasDevice(IDevice device) {
		return devices.contains(device);
	}
	
	public Set<IDevice> getDevices() {
		return Collections.unmodifiableSet(devices);
	}
	
	public BroadcastTree getMulticastTree(DatapathId switchId) {
		return destinationsRootedMulticastTrees.get(switchId);
	}
	
	public void setMulticastTree(DatapathId switchId, BroadcastTree mt) {
		destinationsRootedMulticastTrees.put(switchId, mt);
	}
	
	public Set<DatapathId> getSwitches() {
		return destinationsRootedMulticastTrees.keySet();
	}
	
	public boolean hasSwitch(DatapathId swId) {
		return destinationsRootedMulticastTrees.containsKey(swId);
	}
	
	public Set<OFPort> getDevicePortsOfSwitch(DatapathId swId) {
		Set<OFPort> ret = new HashSet<OFPort>();
		for (IDevice device: devices) {
			for (SwitchPort sp: device.getAttachmentPoints()) {
				DatapathId _swId = sp.getNodeId();
				if (swId == _swId) {
					ret.add(sp.getPortId());
				}
			}
		}
		return ret;
	}
	
    @Override
    public boolean equals(Object o) {
        if (this == o) {
        	return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
        	return false;
        }

        MulticastGroup that = (MulticastGroup) o;

        if (!id.equals(that.id)) {
        	return false;
        }
        
        if (!archipelago.getId().equals(that.archipelago.getId())) {
        	return false;
        }
        
        if (!devices.isEmpty() && 
        		!devices.equals(that.devices)) {
    		return false;
        }
        
        if (!destinationsRootedMulticastTrees.isEmpty() && 
        		!destinationsRootedMulticastTrees.equals(that.destinationsRootedMulticastTrees)) {
    		return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + archipelago.hashCode();
        result = 31 * result + devices.hashCode();
        result = 31 * result + destinationsRootedMulticastTrees.hashCode();
        return result;
    }
}