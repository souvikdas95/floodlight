package net.floodlightcontroller.topology;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.python.google.common.collect.ImmutableSet;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;

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
	
	// Map of switches and attachment point to multicast devices
	private Map<DatapathId, Set<OFPort>> attachmentPoints;
	
	// Count of duplicate attachment points
	private Map<SwitchPort, Integer> attachmentPointCount;
	
	public MulticastGroup(DatapathId mgId, Archipelago archipelago) {
		this.mgId = mgId;
		this.archipelago = archipelago;
		devices = new HashSet<IDevice>();
		attachmentPoints = new HashMap<DatapathId, Set<OFPort>>();
		attachmentPointCount = new HashMap<SwitchPort, Integer>();
	}
	
	public DatapathId getId() {
		return mgId;
	}
	
	public Archipelago getArchiepelago() {
		return archipelago;
	}
	
	public void addDevice(IDevice device) {
		removeDevice(device);
		devices.add(device);
		Set<DatapathId> validSw = archipelago.getSwitches();
		for (SwitchPort sp: device.getAttachmentPoints()) {
			Integer count = attachmentPointCount.get(sp);
			if (count == null) {
				DatapathId swId = sp.getNodeId();
				if (validSw.contains(swId)) {
					OFPort port = sp.getPortId();
					Set<OFPort> ports = attachmentPoints.get(swId);
					if (ports == null) {
						ports = new HashSet<OFPort>();
						attachmentPoints.put(swId, ports);
					}
					ports.add(port);
				}
				attachmentPointCount.put(sp, 1);
			}
			else {
				attachmentPointCount.put(sp, count + 1);
			}
		}
	}
	
	public void removeDevice(IDevice device) {
		if (devices.contains(device)) {
			devices.remove(device);
			for (SwitchPort sp: device.getAttachmentPoints()) {
				Integer count = attachmentPointCount.get(sp);
				if (count <= 1) {
					DatapathId swId = sp.getNodeId();
					Set<OFPort> ports = attachmentPoints.get(swId);
					if (ports != null) {
						OFPort port = sp.getPortId();
						ports.remove(port);
						if (ports.isEmpty()) {
							attachmentPoints.remove(swId);
						}
					}
					attachmentPointCount.remove(sp);
				}
				else {
					attachmentPointCount.put(sp, count - 1);
				}
			}
		}
	}
	
	public boolean hasDevice(IDevice device) {
		return devices.contains(device);
	}
	
	public Set<IDevice> getDevices() {
		return Collections.unmodifiableSet(devices);
	}
	
	public Set<DatapathId> getSwitches() {
		return Collections.unmodifiableSet(attachmentPoints.keySet());
	}
	
	public Set<OFPort> getAttachmentPoints(DatapathId swId) {
		if (!attachmentPoints.containsKey(swId)) {
			return ImmutableSet.of();
		}
		return Collections.unmodifiableSet(attachmentPoints.get(swId));
	}
	
	public boolean hasSwitch(DatapathId swId) {
		return attachmentPoints.containsKey(swId);
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
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = mgId.hashCode();
        result = 31 * result + archipelago.hashCode();
        return result;
    }
}