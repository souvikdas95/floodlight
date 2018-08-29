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
import net.floodlightcontroller.util.RWSync;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Group of Multicasting Devices and their attachmentPoints
 * in a given archipelago
 * 
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
	
	// Reader-Writer Sync
	private final RWSync rwSync;
	
	public MulticastGroup(BigInteger mgId, Archipelago archipelago) {
		this.mgId = mgId;
		this.archipelago = archipelago;
		devAps = new HashMap<Long, Set<NodePortTuple>>();
		apDevs = new HashMap<NodePortTuple, Set<Long>>();
		swEdgePorts = new HashMap<DatapathId, Set<OFPort>>();
		rwSync = new RWSync();
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
		
		rwSync.writeLock();
		
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
		
		rwSync.writeUnlock();
	}
	
	public void remove(Long devKey) {
		if (devKey == null) {
			return;
		}
		
		rwSync.writeLock();
		
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
		
		rwSync.writeUnlock();
	}
	
	public void remove(Long devKey, NodePortTuple npt) {
		if (devKey == null || npt == null) {
			return;
		}
		
		rwSync.writeLock();
		
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
		
		rwSync.writeUnlock();
	}
	
	public boolean hasDevice(Long devKey) {
		if (devKey == null) {
			return false;
		}
		
		boolean result;
		
		rwSync.readLock();
		
		result = devAps.keySet().contains(devKey);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public boolean hasAttachmentPoint(NodePortTuple npt) {
		if (npt == null) {
			return false;
		}
		
		boolean result;
		
		rwSync.readLock();
		
		result = apDevs.keySet().contains(npt);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<Long> getAllDevices() {
		Set<Long> result;
		
		rwSync.readLock();
		
		result =  new HashSet<Long>(devAps.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<NodePortTuple> getAllAttachmentPoints() {
		Set<NodePortTuple> result;
		
		rwSync.readLock();
		
		result = Collections.unmodifiableSet(apDevs.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<Long> getDevices(NodePortTuple npt) {
		if (npt == null) {
			return ImmutableSet.of();
		}
		
		Set<Long> result;
		
		rwSync.readLock();
		
		result = apDevs.get(npt);
		result = (result == null) ? ImmutableSet.of() : new HashSet<Long>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<NodePortTuple> getAttachmentPoints(Long devKey) {
		if (devKey == null) {
			return ImmutableSet.of();
		}
		
		Set<NodePortTuple> result;
		
		rwSync.readLock();
		
		result = devAps.get(devKey);
		result = (result == null) ? ImmutableSet.of() : new HashSet<NodePortTuple>(result);
		
		rwSync.readUnlock();
		
		return (result == null) ? ImmutableSet.of() : Collections.unmodifiableSet(result);
	}
	
	public boolean hasSwitch(DatapathId swId) {
		if (swId == null) {
			return false;
		}
		
		boolean result;
		
		rwSync.readLock();
		
		result = swEdgePorts.containsKey(swId);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<DatapathId> getSwitches() {
		Set<DatapathId> result;
		
		rwSync.readLock();
		
		result =  new HashSet<DatapathId>(swEdgePorts.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<OFPort> getEdgePorts(DatapathId swId) {
		if (swId == null) {
			return ImmutableSet.of();
		}
		
		Set<OFPort> result;
		
		rwSync.readLock();
		
		result = swEdgePorts.get(swId);
		result = (result == null) ? ImmutableSet.of() : new HashSet<OFPort>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public boolean isEmpty() {
		boolean result;
		
		rwSync.readLock();
		
		result = devAps.isEmpty();
		
		rwSync.readUnlock();
		
		return result;
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
