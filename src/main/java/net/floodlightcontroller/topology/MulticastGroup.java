package net.floodlightcontroller.topology;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.python.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.types.NodePortTuple;

/*
 * Must exist per archipelago
 */
public class MulticastGroup {
	
	// Multicast Group Id
	private final BigInteger mgId;
	
	// Parent Archipelago (only used for reference & hashCode)
	private final Archipelago archipelago;
	
	// Map of devices and participant attachment points in archipelago
	private final Map<Long, Set<NodePortTuple>> devAps;
	
	// Map of attachment point and participant devices in archipelago
	private final Map<NodePortTuple, Set<Long>> apDevs;
	
	// Map of switches and edge ports connected to participant devices
	private final Map<DatapathId, Set<OFPort>> swEdgePorts;
	
	public MulticastGroup(BigInteger mgId, Archipelago archipelago) {
		this.mgId = mgId;
		this.archipelago = archipelago;
		devAps = new HashMap<Long, Set<NodePortTuple>>();
		apDevs = new HashMap<NodePortTuple, Set<Long>>();
		swEdgePorts = new HashMap<DatapathId, Set<OFPort>>();
	}
	
	public BigInteger getId() {
		return mgId;
	}
	
	public Archipelago getArchiepelago() {
		return archipelago;
	}
	
	public void add(Long devKey, NodePortTuple ap) {
		if (devKey == null || ap == null) {
			return;
		}
		
		Set<NodePortTuple> nptSet = devAps.get(devKey);
		if (nptSet == null) {
			nptSet = new HashSet<NodePortTuple>();
			devAps.put(devKey, nptSet);
		}
		nptSet.add(ap);
		
		Set<Long> devSet = apDevs.get(ap);
		if (devSet == null) {
			devSet = new HashSet<Long>();
			apDevs.put(ap, devSet);
		}
		devSet.add(devKey);
		
		DatapathId swId = ap.getNodeId();
		OFPort port = ap.getPortId();
		Set<OFPort> ports = swEdgePorts.get(swId);
		if (ports == null) {
			ports = new HashSet<OFPort>();
			swEdgePorts.put(swId, ports);
		}
		ports.add(port);
	}
	
	public void remove(Long devKey) {
		if (devKey == null) {
			return;
		}
		
		Set<NodePortTuple> nptSet = devAps.get(devKey);
		if (nptSet != null) {
			for (NodePortTuple npt: nptSet) {
				Set<Long> devSet = apDevs.get(npt);
				devSet.remove(devKey);
				if (devSet.isEmpty()) {
					apDevs.remove(npt);
					DatapathId swId = npt.getNodeId();
					OFPort port = npt.getPortId();
					Set<OFPort> ports = swEdgePorts.get(swId);
					ports.remove(port);
					if (ports.isEmpty()) {
						swEdgePorts.remove(swId);
					}
				}
			}
			devAps.remove(devKey);
		}
	}
	
	public void remove(Long devKey, NodePortTuple npt) {
		if (devKey == null || npt == null) {
			return;
		}
		
		Set<NodePortTuple> nptSet = devAps.get(devKey);
		if (nptSet != null) {
			if (nptSet.contains(npt)) {
				Set<Long> devSet = apDevs.get(npt);
				devSet.remove(devKey);
				if (devSet.isEmpty()) {
					apDevs.remove(npt);
					DatapathId swId = npt.getNodeId();
					OFPort port = npt.getPortId();
					Set<OFPort> ports = swEdgePorts.get(swId);
					ports.remove(port);
					if (ports.isEmpty()) {
						swEdgePorts.remove(swId);
					}
				}
				nptSet.remove(npt);
			}
			if (nptSet.isEmpty()) {
				devAps.remove(devKey);
			}
		}
	}
	
	public boolean hasDevice(Long devKey) {
		if (devKey == null) {
			return false;
		}
		
		return devAps.keySet().contains(devKey);
	}
	
	public boolean hasAttachmentPoint(NodePortTuple npt) {
		if (npt == null) {
			return false;
		}
		
		return apDevs.keySet().contains(npt);
	}
	
	public Set<Long> getAllDevices() {
		return Collections.unmodifiableSet(devAps.keySet());
	}
	
	public Set<NodePortTuple> getAllAttachmentPoints() {
		return Collections.unmodifiableSet(apDevs.keySet());
	}
	
	public Set<Long> getDevices(NodePortTuple npt) {
		if (npt == null) {
			return ImmutableSet.of();
		}
		
		Set<Long> result = apDevs.get(npt);
		return (result == null) ? ImmutableSet.of() : Collections.unmodifiableSet(result);
	}
	
	public Set<NodePortTuple> getAttachmentPoints(Long devKey) {
		if (devKey == null) {
			return ImmutableSet.of();
		}
		
		Set<NodePortTuple> result = devAps.get(devKey);
		return (result == null) ? ImmutableSet.of() : Collections.unmodifiableSet(result);
	}
	
	public boolean hasSwitch(DatapathId swId) {
		if (swId == null) {
			return false;
		}
		
		return swEdgePorts.containsKey(swId);
	}
	
	public Set<DatapathId> getSwitches() {
		return Collections.unmodifiableSet(swEdgePorts.keySet());
	}
	
	public Set<OFPort> getEdgePorts(DatapathId swId) {
		if (swId == null) {
			return ImmutableSet.of();
		}
		
		Set<OFPort> result = swEdgePorts.get(swId);
		return (result == null) ? ImmutableSet.of() : Collections.unmodifiableSet(result);
	}
	
	public boolean isEmpty() {
		return devAps.isEmpty();
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
