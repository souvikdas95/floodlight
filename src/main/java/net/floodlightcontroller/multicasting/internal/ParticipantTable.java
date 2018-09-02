package net.floodlightcontroller.multicasting.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import org.projectfloodlight.openflow.types.IPAddress;
import org.python.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.util.RWSync;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Participant table that maps b/w Multicast IP, Device Intf & AttachmentPoints
 * 
 */
public class ParticipantTable {
	private class Group {
		private final IPAddress<?> groupAddress;
		private final MacVlanPair intf;
		protected Group(IPAddress<?> groupAddress, MacVlanPair intf) {
			this.groupAddress = groupAddress;
			this.intf = intf;
		}
		protected IPAddress<?> getGroupAddress() {
			return groupAddress;
		}
		protected MacVlanPair getIntf() {
			return intf;
		}
		@Override
	    public boolean equals(Object o) {
			if (this == o) {
	        	return true;
	        }
	        if (o == null || getClass() != o.getClass()) {
	        	return false;
	        }
	        Group that = (Group) o;
	        if (groupAddress == null || that.groupAddress == null || 
	        		!groupAddress.equals(that.groupAddress)) {
	        	return false;
	        }
	        if (intf == null || that.intf == null || 
	        		!intf.equals(that.intf)) {
	        	return false;
	        }
	        return true;
	    }
	    @Override
	    public int hashCode() {
	        int result = groupAddress.hashCode();
	        result = 31 * result + intf.hashCode();
	        return result;
	    }
	}
	
	private final Map<IPAddress<?>, Set<MacVlanPair>> groupAddressToIntfMap;
	private final Map<MacVlanPair, Set<IPAddress<?>>> intfToGroupAddressMap;
	
	private final Map<Group, Set<NodePortTuple>> groupToApMap;
	
	// Reader-Writer Synchronization Class & Object
	private final RWSync rwSync;
	
	public ParticipantTable() {
		groupAddressToIntfMap = new HashMap<IPAddress<?>, Set<MacVlanPair>>();
		intfToGroupAddressMap = new HashMap<MacVlanPair, Set<IPAddress<?>>>();
		
		groupToApMap = new HashMap<Group, Set<NodePortTuple>>();
		
		rwSync = new RWSync();
	}
	
	public void add(IPAddress<?> groupAddress, MacVlanPair intf, NodePortTuple ap) {
		if (groupAddress == null || intf == null || ap == null) {
			return;
		}
		
		Set<MacVlanPair> intfSet;
		Set<IPAddress<?>> groupAddressSet;
		Set<NodePortTuple> apSet;
		Group group = new Group(groupAddress, intf);

		rwSync.writeLock();
		
		intfSet = groupAddressToIntfMap.get(groupAddress);
		if (intfSet == null) {
			intfSet = new HashSet<MacVlanPair>();
			groupAddressToIntfMap.put(groupAddress, intfSet);
		}
		intfSet.add(intf);
		
		groupAddressSet = intfToGroupAddressMap.get(intf);
		if (groupAddressSet == null) {
			groupAddressSet = new HashSet<IPAddress<?>>();
			intfToGroupAddressMap.put(intf, groupAddressSet);
		}
		groupAddressSet.add(groupAddress);
		
		apSet = groupToApMap.get(group);
		if (apSet == null) {
			apSet = new HashSet<NodePortTuple>();
			groupToApMap.put(group, apSet);
		}
		apSet.add(ap);
		
		rwSync.writeUnlock();
	}
	
	public void remove(IPAddress<?> groupAddress, MacVlanPair intf, NodePortTuple ap) {
		if (groupAddress == null || intf == null || ap == null) {
			return;
		}
		
		Set<MacVlanPair> intfSet;
		Set<IPAddress<?>> groupAddressSet;
		Set<NodePortTuple> apSet;
		Group group = new Group(groupAddress, intf);

		rwSync.writeLock();
		
		apSet = groupToApMap.get(group);
		if (apSet != null) {
			apSet.remove(ap);
			if (apSet.isEmpty()) {
				groupToApMap.remove(group);

				intfSet = groupAddressToIntfMap.get(groupAddress);
				if (intfSet != null) {
					intfSet.remove(intf);
					if (intfSet.isEmpty()) {
						groupAddressToIntfMap.remove(groupAddress);
					}
				}

				groupAddressSet = intfToGroupAddressMap.get(intf);
				if (groupAddressSet != null) {
					groupAddressSet.remove(groupAddress);
					if (groupAddressSet.isEmpty()) {
						intfToGroupAddressMap.remove(intf);
					}
				}
			}
		}
		
		rwSync.writeUnlock();
	}
	
	public Boolean contains(IPAddress<?> groupAddress, MacVlanPair intf) {
		if (groupAddress == null || intf == null) {
			return false;
		}
		
		Boolean result;
		Group group = new Group(groupAddress, intf);
		
		rwSync.readLock();
		
		result = groupToApMap.containsKey(group);
		
		rwSync.readUnlock();

		return result;
	}
	
	public Set<NodePortTuple> getAttachmentPoints(IPAddress<?> groupAddress, MacVlanPair intf) {
		Set<NodePortTuple> result;
		Group group = new Group(groupAddress, intf);
		
		rwSync.readLock();
		
		result = groupToApMap.get(group);
		result = (result == null) ? ImmutableSet.of() : new HashSet<NodePortTuple>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<MacVlanPair> getIntfs(IPAddress<?> groupAddress) {
		if (groupAddress == null) {
			return ImmutableSet.of();
		}
		
		Set<MacVlanPair> result;
		
		rwSync.readLock();
		
		result = groupAddressToIntfMap.get(groupAddress);
		result = (result == null) ? ImmutableSet.of() : new HashSet<MacVlanPair>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<IPAddress<?>> getGroupAddresses(MacVlanPair intf) {
		if (intf == null) {
			return ImmutableSet.of();
		}
		
		Set<IPAddress<?>> result;
		
		rwSync.readLock();
		
		result = intfToGroupAddressMap.get(intf);
		result = (result == null) ? ImmutableSet.of() : new HashSet<IPAddress<?>>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<MacVlanPair> getAllIntfs() {
		Set<MacVlanPair> result;
		
		rwSync.readLock();
		
		result = new HashSet<MacVlanPair>(intfToGroupAddressMap.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<IPAddress<?>> getAllGroupAddresses() {
		Set<IPAddress<?>> result;
		
		rwSync.readLock();
		
		result = new HashSet<IPAddress<?>>(groupAddressToIntfMap.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Boolean hasIntf(MacVlanPair intf) {
		if (intf == null) {
			return false;
		}
		
		Boolean result;
		
		rwSync.readLock();
		
		result = intfToGroupAddressMap.containsKey(intf);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Boolean hasGroupAddress(IPAddress<?> groupAddress) {
		if (groupAddress == null) {
			return false;
		}
		
		Boolean result;
		
		rwSync.readLock();
		
		result = groupAddressToIntfMap.containsKey(groupAddress);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public void deleteGroupAddress(IPAddress<?> groupAddress) {
		if (groupAddress == null) {
			return;
		}
		
		Set<MacVlanPair> intfSet = getIntfs(groupAddress);
		for (MacVlanPair intf: intfSet) {
			Set<NodePortTuple> apSet = getAttachmentPoints(groupAddress, intf);
			for (NodePortTuple ap: apSet) {
				remove(groupAddress, intf, ap);
			}
		}
	}
	
	public void deleteIntf(MacVlanPair intf) {
		if (intf == null) {
			return;
		}
		
		Set<IPAddress<?>> groupAddressSet = getGroupAddresses(intf);
		for (IPAddress<?> groupAddress: groupAddressSet) {
			Set<NodePortTuple> apSet = getAttachmentPoints(groupAddress, intf);
			for (NodePortTuple ap: apSet) {
				remove(groupAddress, intf, ap);
			}
		}
	}
	
	public void clearTable() {
		rwSync.writeLock();
		
		groupAddressToIntfMap.clear();
		intfToGroupAddressMap.clear();
		groupToApMap.clear();
		
		rwSync.writeUnlock();
	}
}