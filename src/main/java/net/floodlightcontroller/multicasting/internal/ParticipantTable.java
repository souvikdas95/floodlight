package net.floodlightcontroller.multicasting.internal;

import java.util.Collections;
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
		if (!mcastToDeviceMap.containsKey(mcastAddress)) {
			mcastToDeviceMap.put(mcastAddress, ConcurrentHashMap.newKeySet());
		}
		Set<IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
		if (!memberSet.contains(device)) {
			memberSet.add(device);
		}
		
		// Add to Group Set
		if (!deviceToMcastMap.containsKey(device)) {
			deviceToMcastMap.put(device, ConcurrentHashMap.newKeySet());
		}
		Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);
		if (!groupSet.contains(mcastAddress)) {
			groupSet.add(mcastAddress);
		}
	}
	
	public void remove(IPAddress<?> mcastAddress, IDevice device) {
		if (mcastAddress == null || device == null) {
			return;
		}
		
		// Remove from Member Set
		if (mcastToDeviceMap.containsKey(mcastAddress)) {
			Set<IDevice> memberSet  = mcastToDeviceMap.get(mcastAddress);
			if (memberSet.contains(device)) {
				memberSet.remove(device);
				if (memberSet.isEmpty()) {
					mcastToDeviceMap.remove(mcastAddress);
				}
			}
		}
		
		// Remove from Group Set
		if (deviceToMcastMap.containsKey(device)) {
			Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);
			if (groupSet.contains(mcastAddress)) {
				groupSet.remove(mcastAddress);
				if (groupSet.isEmpty()) {
					deviceToMcastMap.remove(device);
				}
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
		if (mcastToDeviceMap.containsKey(mcastAddress)) {
			Set<IDevice> memberSet = ConcurrentHashMap.newKeySet();
			memberSet.addAll(mcastToDeviceMap.get(mcastAddress));
			return Collections.unmodifiableSet(memberSet);
		}
		return ImmutableSet.of();
	}
	
	public Set<IPAddress<?>> getGroups(IDevice device) {
		if (device == null) {
			return ImmutableSet.of();
		}
		if (deviceToMcastMap.containsKey(device)) {
			Set<IPAddress<?>> groupSet = ConcurrentHashMap.newKeySet();
			groupSet.addAll(deviceToMcastMap.get(device));
			return Collections.unmodifiableSet(groupSet);
		}
		return ImmutableSet.of();
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
		if (mcastToDeviceMap.containsKey(mcastAddress)) {
			Set<IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
			for(IDevice device: memberSet) {
				remove(mcastAddress, device);
			}
		}
	}
	
	public void deleteMember(IDevice device) {
		if (device == null) {
			return;
		}
		if (deviceToMcastMap.containsKey(device)) {
			Set<IPAddress<?>> groupSet = deviceToMcastMap.get(device);			
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