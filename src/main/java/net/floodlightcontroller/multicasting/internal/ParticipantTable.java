package net.floodlightcontroller.multicasting.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import org.projectfloodlight.openflow.types.IPAddress;
import org.python.google.common.collect.ImmutableSet;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.util.RWSync;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Participant table that maps b/w Multicast IP and Devices
 * 
 */
public class ParticipantTable {
	private final Map<IPAddress<?>, Set<IDevice>> mcastToDeviceMap;
	private final Map<IDevice, Set<IPAddress<?>>> deviceToMcastMap;
	
	// Reader-Writer Synchronization Class & Object
	private final RWSync rwSync;
	
	public ParticipantTable() {
		mcastToDeviceMap = new HashMap<IPAddress<?>, Set<IDevice>>();
		deviceToMcastMap = new HashMap<IDevice, Set<IPAddress<?>>>();
		
		rwSync = new RWSync();
	}
	
	public void add(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return;
		}
		
		// Init
		Set<IDevice> memberSet;
		Set<IPAddress<?>> groupSet;
		
		// Acquire lock
		rwSync.writeLock();
		
		// Add to Member Set
		memberSet = mcastToDeviceMap.get(mcastAddress);
		if (memberSet == null) {
			memberSet = new HashSet<IDevice>();
			mcastToDeviceMap.put(mcastAddress, memberSet);
		}
		memberSet.add(device);
		
		// Add to Group Set
		groupSet = deviceToMcastMap.get(device);
		if (groupSet == null) {
			groupSet = new HashSet<IPAddress<?>>();
			deviceToMcastMap.put(device, groupSet);
		}
		groupSet.add(mcastAddress);
		
		// Release lock
		rwSync.writeUnlock();
	}
	
	public void remove(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return;
		}
		
		// Init
		Set<IDevice> memberSet;
		Set<IPAddress<?>> groupSet;
		
		// Acquire lock
		rwSync.writeLock();
	
		// Remove from Member Set
		memberSet = mcastToDeviceMap.get(mcastAddress);
		if (memberSet != null) {
			memberSet.remove(device);
			if (memberSet.isEmpty()) {
				mcastToDeviceMap.remove(mcastAddress);
			}
		}
		
		// Remove from Group Set
		groupSet = deviceToMcastMap.get(device);
		if (groupSet != null) {
			groupSet.remove(mcastAddress);
			if (groupSet.isEmpty()) {
				deviceToMcastMap.remove(device);
			}
		}
		
		// Release lock
		rwSync.writeUnlock();
	}
	
	public Boolean contains(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return false;
		}
		
		// Init
		Set<IDevice> memberSet;
		Boolean result;
		
		// Acquire lock
		rwSync.readLock();
		
		result = false;
		memberSet = mcastToDeviceMap.get(mcastAddress);
		if (memberSet != null &&
				memberSet.contains(device)) {
			result = true;
		}
		
		// Release lock
		rwSync.readUnlock();

		return result;
	}
	
	public Set<IDevice> getMembers(IPAddress<?> mcastAddress) {
		if (mcastAddress == null) {
			return ImmutableSet.of();
		}
		
		// Init
		Set<IDevice> memberSet;
		
		// Acquire lock
		rwSync.readLock();
		
		memberSet = mcastToDeviceMap.get(mcastAddress);
		memberSet = (memberSet == null) ? ImmutableSet.of() : new HashSet<IDevice>(memberSet);
		
		// Release lock
		rwSync.readUnlock();
		
		return memberSet;
	}
	
	public Set<IPAddress<?>> getGroups(IDevice device) {
		if (device == null) {
			return ImmutableSet.of();
		}
		
		// Init
		Set<IPAddress<?>> groupSet;
		
		// Acquire lock
		rwSync.readLock();
		
		groupSet = deviceToMcastMap.get(device);
		groupSet = (groupSet == null) ? ImmutableSet.of() : new HashSet<IPAddress<?>>(groupSet);
		
		// Release lock
		rwSync.readUnlock();
		
		return groupSet;
	}
	
	public Set<IDevice> getAllMembers() {
		// Init
		Set<IDevice> allMemberSet;
		
		// Acquire lock
		rwSync.readLock();
		
		allMemberSet = new HashSet<IDevice>(deviceToMcastMap.keySet());
		
		// Release lock
		rwSync.readUnlock();
		
		return allMemberSet;
	}
	
	public Set<IPAddress<?>> getAllGroups() {
		// Init
		Set<IPAddress<?>> allGroupSet;
		
		// Acquire lock
		rwSync.readLock();
		
		allGroupSet = Collections.unmodifiableSet(mcastToDeviceMap.keySet());
		
		// Release lock
		rwSync.readUnlock();
		
		return allGroupSet;
	}
	
	public Boolean isMember(IDevice device) {
		if (device == null) {
			return false;
		}
		
		// Init
		Boolean result;
		
		// Acquire lock
		rwSync.readLock();
		
		result = deviceToMcastMap.containsKey(device);
		
		// Release lock
		rwSync.readUnlock();
		
		return result;
	}
	
	public Boolean isGroup(IPAddress<?> mcastAddress) {
		if (mcastAddress == null) {
			return false;
		}
		
		// Init
		Boolean result;
		
		// Acquire lock
		rwSync.readLock();
		
		result = mcastToDeviceMap.containsKey(mcastAddress);
		
		// Release lock
		rwSync.readUnlock();
		
		return result;
	}
	
	public void deleteGroup(IPAddress<?> mcastAddress) {
		if (mcastAddress == null) {
			return;
		}
		
		Set<IDevice> memberSet = getMembers(mcastAddress);
		if (memberSet != null) {
			for(IDevice device: memberSet) {
				remove(mcastAddress, device);
			}
		}
	}
	
	public void deleteMember(IDevice device) {
		if (device == null) {
			return;
		}
		
		Set<IPAddress<?>> groupSet = getGroups(device);
		if (groupSet != null) {	
			for(IPAddress<?> mcastAddress: groupSet) {
				remove(mcastAddress, device);
			}
		}
	}
	
	public void clearTable() {
		// Acquire lock
		rwSync.writeLock();
		
		mcastToDeviceMap.clear();
		deviceToMcastMap.clear();
		
		// Release lock
		rwSync.writeUnlock();
	}
}