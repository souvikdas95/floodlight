package net.floodlightcontroller.topology;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.python.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;

/*
 * Must exist per archipelago
 */
public class MulticastGroup {
	
	// Multicast Group Id
	private BigInteger mgId;
	
	// Parent Archipelago (only used for reference & hashCode)
	private Archipelago archipelago;
	
	// Set of Devices that belong to this Multicast Group
	private Set<IDevice> devices;
	
	// Map of switches and attachment point to multicast devices
	private Map<DatapathId, Set<OFPort>> attachmentPoints;
	
	// Count of duplicate attachment points
	private Map<NodePortTuple, Integer> attachmentPointsCount;
	
	public MulticastGroup(BigInteger mgId, Archipelago archipelago) {
		this.mgId = mgId;
		this.archipelago = archipelago;
		devices = new HashSet<IDevice>();
		attachmentPoints = new HashMap<DatapathId, Set<OFPort>>();
		attachmentPointsCount = new HashMap<NodePortTuple, Integer>();
	}
	
	public BigInteger getId() {
		return mgId;
	}
	
	public void setId(BigInteger mgId) {
		this.mgId = mgId;
	}
	
	public Archipelago getArchiepelago() {
		return archipelago;
	}
	
	public void setArchipelago(Archipelago archipelago) {
		this.archipelago = archipelago;
	}
	
	public void addDevice(IDevice device) {
		if (!devices.contains(device)) {
			Set<DatapathId> validSw = archipelago.getSwitches();
			for (NodePortTuple npt: device.getAttachmentPoints()) {
				Integer count = attachmentPointsCount.get(npt);
				if (count == null) {
					DatapathId swId = npt.getNodeId();
					if (validSw.contains(swId)) {
						OFPort port = npt.getPortId();
						Set<OFPort> ports = attachmentPoints.get(swId);
						if (ports == null) {
							ports = new HashSet<OFPort>();
							attachmentPoints.put(swId, ports);
						}
						ports.add(port);
					}
					attachmentPointsCount.put(npt, 1);
				}
				else {
					attachmentPointsCount.put(npt, count + 1);
				}
			}
			devices.add(device);
		}
	}
	
	public void removeDevice(IDevice device) {
		if (devices.contains(device)) {
			for (NodePortTuple npt: device.getAttachmentPoints()) {
				Integer count = attachmentPointsCount.get(npt);
				if (count <= 1) {
					DatapathId swId = npt.getNodeId();
					Set<OFPort> ports = attachmentPoints.get(swId);
					if (ports != null) {
						OFPort port = npt.getPortId();
						ports.remove(port);
						if (ports.isEmpty()) {
							attachmentPoints.remove(swId);
						}
					}
					attachmentPointsCount.remove(npt);
				}
				else {
					attachmentPointsCount.put(npt, count - 1);
				}
			}
			devices.remove(device);
		}
	}
	
	public boolean hasDevice(IDevice device) {
		return devices.contains(device);
	}
	
	public Set<IDevice> getDevices() {
		return Collections.unmodifiableSet(devices);
	}
	
	public boolean hasAttachmentPoint(DatapathId swId, OFPort port) {
		return hasAttachmentPoint(new NodePortTuple(swId, port));
	}
	
	public boolean hasAttachmentPoint(NodePortTuple npt) {
		return attachmentPointsCount.containsKey(npt);
	}
	
	public Set<OFPort> getAttachmentPoints(DatapathId swId) {
		Set<OFPort> result = attachmentPoints.get(swId);
		return (result == null) ? ImmutableSet.of() : Collections.unmodifiableSet(result);
	}
	
	public Set<NodePortTuple> getAllAttachmentPoints() {
		return Collections.unmodifiableSet(attachmentPointsCount.keySet());
	}
	
	public Set<DatapathId> getSwitches() {
		return Collections.unmodifiableSet(attachmentPoints.keySet());
	}
	
	public boolean hasSwitch(DatapathId swId) {
		return attachmentPoints.containsKey(swId);
	}
	
	public boolean isEmpty() {
		return devices.isEmpty();
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
       
        if (mgId == null || that.mgId == null || 
        		!mgId.equals(that.mgId)) {
        	return false;
        }
        
        if (archipelago == null || that.archipelago == null || 
        		!archipelago.getId().equals(that.archipelago.getId())) {
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
