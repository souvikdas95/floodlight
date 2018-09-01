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
	private class Member {
		private final IPAddress<?> group;
		private final MacVlanPair intf;
		protected Member(IPAddress<?> group, MacVlanPair intf) {
			this.group = group;
			this.intf = intf;
		}
		protected IPAddress<?> getGroup() {
			return group;
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
	        Member that = (Member) o;
	        if (group == null || that.group == null || 
	        		!group.equals(that.group)) {
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
	        int result = group.hashCode();
	        result = 31 * result + intf.hashCode();
	        return result;
	    }
	}
	
	private final Map<IPAddress<?>, Set<MacVlanPair>> groupToIntfMap;
	private final Map<MacVlanPair, Set<IPAddress<?>>> intfToGroupMap;
	
	private final Map<Member, Set<NodePortTuple>> memberToApMap;
	
	// Reader-Writer Synchronization Class & Object
	private final RWSync rwSync;
	
	public ParticipantTable() {
		groupToIntfMap = new HashMap<IPAddress<?>, Set<MacVlanPair>>();
		intfToGroupMap = new HashMap<MacVlanPair, Set<IPAddress<?>>>();
		
		memberToApMap = new HashMap<Member, Set<NodePortTuple>>();
		
		rwSync = new RWSync();
	}
	
	public void add(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap) {
		if (group == null || intf == null || ap == null) {
			return;
		}
		
		Set<MacVlanPair> intfSet;
		Set<IPAddress<?>> groupSet;
		Set<NodePortTuple> apSet;
		Member member = new Member(group, intf);

		rwSync.writeLock();
		
		intfSet = groupToIntfMap.get(group);
		if (intfSet == null) {
			intfSet = new HashSet<MacVlanPair>();
			groupToIntfMap.put(group, intfSet);
		}
		intfSet.add(intf);
		
		groupSet = intfToGroupMap.get(intf);
		if (groupSet == null) {
			groupSet = new HashSet<IPAddress<?>>();
			intfToGroupMap.put(intf, groupSet);
		}
		groupSet.add(group);
		
		apSet = memberToApMap.get(member);
		if (apSet == null) {
			apSet = new HashSet<NodePortTuple>();
			memberToApMap.put(member, apSet);
		}
		apSet.add(ap);
		
		rwSync.writeUnlock();
	}
	
	public void remove(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap) {
		if (group == null || intf == null || ap == null) {
			return;
		}
		
		Set<MacVlanPair> intfSet;
		Set<IPAddress<?>> groupSet;
		Set<NodePortTuple> apSet;
		Member member = new Member(group, intf);

		rwSync.writeLock();
		
		apSet = memberToApMap.get(member);
		if (apSet != null) {
			apSet.remove(ap);
			if (apSet.isEmpty()) {
				memberToApMap.remove(member);

				intfSet = groupToIntfMap.get(group);
				if (intfSet != null) {
					intfSet.remove(intf);
					if (intfSet.isEmpty()) {
						groupToIntfMap.remove(group);
					}
				}

				groupSet = intfToGroupMap.get(intf);
				if (groupSet != null) {
					groupSet.remove(group);
					if (groupSet.isEmpty()) {
						intfToGroupMap.remove(intf);
					}
				}
			}
		}
		
		rwSync.writeUnlock();
	}
	
	public Boolean contains(IPAddress<?> group, MacVlanPair intf) {
		if (group == null || intf == null) {
			return false;
		}
		
		Boolean result;
		Member member = new Member(group, intf);
		
		rwSync.readLock();
		
		result = memberToApMap.containsKey(member);
		
		rwSync.readUnlock();

		return result;
	}
	
	public Set<NodePortTuple> getAttachmentPoints(IPAddress<?> group, MacVlanPair intf) {
		Set<NodePortTuple> result;
		Member member = new Member(group, intf);
		
		rwSync.readLock();
		
		result = memberToApMap.get(member);
		result = (result == null) ? ImmutableSet.of() : new HashSet<NodePortTuple>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<MacVlanPair> getIntfs(IPAddress<?> group) {
		if (group == null) {
			return ImmutableSet.of();
		}
		
		Set<MacVlanPair> result;
		
		rwSync.readLock();
		
		result = groupToIntfMap.get(group);
		result = (result == null) ? ImmutableSet.of() : new HashSet<MacVlanPair>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<IPAddress<?>> getGroups(MacVlanPair intf) {
		if (intf == null) {
			return ImmutableSet.of();
		}
		
		Set<IPAddress<?>> result;
		
		rwSync.readLock();
		
		result = intfToGroupMap.get(intf);
		result = (result == null) ? ImmutableSet.of() : new HashSet<IPAddress<?>>(result);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<MacVlanPair> getAllIntfs() {
		Set<MacVlanPair> result;
		
		rwSync.readLock();
		
		result = new HashSet<MacVlanPair>(intfToGroupMap.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Set<IPAddress<?>> getAllGroups() {
		Set<IPAddress<?>> result;
		
		rwSync.readLock();
		
		result = new HashSet<IPAddress<?>>(groupToIntfMap.keySet());
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Boolean hasIntf(MacVlanPair intf) {
		if (intf == null) {
			return false;
		}
		
		Boolean result;
		
		rwSync.readLock();
		
		result = intfToGroupMap.containsKey(intf);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public Boolean hasGroup(IPAddress<?> group) {
		if (group == null) {
			return false;
		}
		
		Boolean result;
		
		rwSync.readLock();
		
		result = groupToIntfMap.containsKey(group);
		
		rwSync.readUnlock();
		
		return result;
	}
	
	public void deleteGroup(IPAddress<?> group) {
		if (group == null) {
			return;
		}
		
		rwSync.writeLock();
		
		Set<MacVlanPair> intfSet = groupToIntfMap.get(group);
		if (intfSet != null) {
			for (MacVlanPair intf: intfSet) {
				Set<IPAddress<?>> groupSet = intfToGroupMap.get(intf);
				groupSet.remove(group);
				if (groupSet.isEmpty()) {
					intfToGroupMap.remove(intf);
				}
			}
			groupToIntfMap.remove(group);
			Set<Member> memberSet = new HashSet<Member>();
			for (Member member: memberToApMap.keySet()) {
				if (member.getGroup().equals(group)) {
					memberSet.add(member);
				}
			}
			memberToApMap.keySet().removeAll(memberSet);
		}
		
		rwSync.writeUnlock();
	}
	
	public void deleteIntf(MacVlanPair intf) {
		if (intf == null) {
			return;
		}
		
		rwSync.writeLock();
		
		Set<IPAddress<?>> groupSet = intfToGroupMap.get(intf);
		if (groupSet != null) {
			for (IPAddress<?> group: groupSet) {
				Set<MacVlanPair> intfSet = groupToIntfMap.get(group);
				intfSet.remove(intf);
				if (intfSet.isEmpty()) {
					groupToIntfMap.remove(group);
				}
			}
			intfToGroupMap.remove(intf);
			Set<Member> memberSet = new HashSet<Member>();
			for (Member member: memberToApMap.keySet()) {
				if (member.getIntf().equals(intf)) {
					memberSet.add(member);
				}
			}
			memberToApMap.keySet().removeAll(memberSet);
		}
		
		rwSync.writeUnlock();
	}
	
	public void clearTable() {
		rwSync.writeLock();
		
		groupToIntfMap.clear();
		intfToGroupMap.clear();
		memberToApMap.clear();
		
		rwSync.writeUnlock();
	}
}