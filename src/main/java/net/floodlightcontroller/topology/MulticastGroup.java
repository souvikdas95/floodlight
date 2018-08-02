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
	private final DatapathId mgId;
	
	// Parent Archipelago (only used for reference & hashCode)
	private final Archipelago archipelago;
	
	// Set of Devices that belong to this Multicast Group
	private Set<IDevice> devices;
	
	public MulticastGroup(DatapathId mgId, Archipelago archipelago) {
		this.mgId = mgId;
		this.archipelago = archipelago;
		devices = new HashSet<IDevice>();
	}
	
	public DatapathId getId() {
		return mgId;
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
	
	public Set<DatapathId> getSwitches() {
		Set<DatapathId> ret = new HashSet<DatapathId>();
		for (IDevice device: devices) {
			for (SwitchPort sp: device.getAttachmentPoints()) {
				DatapathId swId = sp.getNodeId();
				if (archipelago.getSwitches().contains(swId)) {
					ret.add(swId);
				}
			}
		}
		return ret;
	}
	
	public boolean hasSwitch(DatapathId swId) {
		for (IDevice device: devices) {
			for (SwitchPort sp: device.getAttachmentPoints()) {
				DatapathId _swId = sp.getNodeId();
				if (_swId.equals(swId) &&
						archipelago.getSwitches().contains(swId)) {
					return true;
				}
			}
		}
		return false;
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

        if (!mgId.equals(that.mgId)) {
        	return false;
        }
        
        if (!archipelago.getId().equals(that.archipelago.getId())) {
        	return false;
        }
        
        if (!devices.isEmpty() && 
        		!devices.equals(that.devices)) {
    		return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = mgId.hashCode();
        result = 31 * result + archipelago.hashCode();
        result = 31 * result + devices.hashCode();
        return result;
    }
}