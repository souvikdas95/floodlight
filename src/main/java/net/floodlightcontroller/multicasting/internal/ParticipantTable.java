package net.floodlightcontroller.multicasting.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.types.IPAddress;
import org.python.google.common.collect.ImmutableSet;

import net.floodlightcontroller.devicemanager.IDevice;

/*
 * Participant table that persists across
 * all topology instances
 */
public class ParticipantTable {	
	private final Map<IPAddress<?>, Set<IDevice>> mcastToDeviceMap;
	private final Map<IDevice, Set<IPAddress<?>>> deviceToMcastMap;
	
	public ParticipantTable() {
		mcastToDeviceMap = new ConcurrentHashMap<IPAddress<?>, Set<IDevice>>();
		deviceToMcastMap = new ConcurrentHashMap<IDevice, Set<IPAddress<?>>>();
	}
	
	public Set<Map.Entry<IPAddress<?>, Set<IDevice>>> entrySet() {
		return Collections.unmodifiableSet(mcastToDeviceMap.entrySet());
	}
	
	public void add(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return;
		}
		
		// Add to Member Set
		Set<IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
		if (memberSet == null) {
			memberSet = new HashSet<IDevice>();
			mcastToDeviceMap.put(mcastAddress, memberSet);
		}
		memberSet.add(device);
		
		// Add to Group Set
		Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);
		if (groupSet == null) {
			groupSet = new HashSet<IPAddress<?>>();
			deviceToMcastMap.put(device, groupSet);
		}
		groupSet.add(mcastAddress);
	}
	
	public void remove(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return;
		}
		
		// Remove from Member Set
		Set<IDevice> memberSet  = mcastToDeviceMap.get(mcastAddress);
		if (memberSet != null) {
			memberSet.remove(device);
			if (memberSet.isEmpty()) {
				mcastToDeviceMap.remove(mcastAddress);
			}
		}
		
		// Remove from Group Set
		Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);
		if (groupSet != null) {
			groupSet.remove(mcastAddress);
			if (groupSet.isEmpty()) {
				deviceToMcastMap.remove(device);
			}
		}
	}
	
	public boolean contains(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return false;
		}
		
		if (mcastToDeviceMap.containsKey(mcastAddress) && 
				deviceToMcastMap.containsKey(device)) {
			return true;
		}	
		return false;
	}
	
	public Set<IDevice> getMembers(IPAddress<?> mcastAddress) {
		if (mcastAddress == null) {
			return ImmutableSet.of();
		}
		
		Set<IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
		return (memberSet == null) ? ImmutableSet.of() : Collections.unmodifiableSet(memberSet);
	}
	
	public Set<IPAddress<?>> getGroups(IDevice device) {
		if (device == null) {
			return ImmutableSet.of();
		}
		
		Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);
		return (groupSet == null) ? ImmutableSet.of() : Collections.unmodifiableSet(groupSet);
	}
	
	public Set<IDevice> getAllMembers() {
		return Collections.unmodifiableSet(deviceToMcastMap.keySet());
	}
	
	public Set<IPAddress<?>> getAllGroups() {
		return Collections.unmodifiableSet(mcastToDeviceMap.keySet());
	}
	
	public boolean isMember(IDevice device) {
		if (device == null) {
			return false;
		}
		
		return deviceToMcastMap.containsKey(device);
	}
	
	public boolean isGroup(IPAddress<?> mcastAddress) {
		if (mcastAddress == null) {
			return false;
		}
		
		return mcastToDeviceMap.containsKey(mcastAddress);
	}
	
	public void deleteGroup(IPAddress<?> mcastAddress) {
		if (mcastAddress == null) {
			return;
		}
		
		Set<IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
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
		
		Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);
		if (groupSet != null) {	
			for(IPAddress<?> mcastAddress: groupSet) {
				remove(mcastAddress, device);
			}
		}
	}
	
	public void clearTable() {
		mcastToDeviceMap.clear();
		deviceToMcastMap.clear();
	}
}