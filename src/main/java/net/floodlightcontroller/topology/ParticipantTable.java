package net.floodlightcontroller.topology;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.devicemanager.IDevice;

/*
 * Participant table that persists across
 * all topology instances
 */
public class ParticipantTable {	
	private Map<IPAddress<?>, Set<IDevice>> mcastToDeviceMap;
	private Map<IDevice, Set<IPAddress<?>>> deviceToMcastMap;
	
	public ParticipantTable() {
		mcastToDeviceMap = new ConcurrentHashMap<IPAddress<?>, Set<IDevice>>();
		deviceToMcastMap = new ConcurrentHashMap<IDevice, Set<IPAddress<?>>>();
	}
	
	public Set<Map.Entry<IPAddress<?>, Set<IDevice>>> entrySet() {
		return Collections.unmodifiableSet(mcastToDeviceMap.entrySet());
	}
	
	public void add(IPAddress<?> mcastAddress, IDevice device) {
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
	
	public void remove(IPAddress<?> mcastAddress, IDevice device)
	{
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
		if (mcastToDeviceMap.containsKey(mcastAddress) && 
				deviceToMcastMap.containsKey(device)) {
			return true;
		}	
		return false;
	}
	
	public Set<IDevice> getMembers(IPAddress<?> mcastAddress) {
		if (mcastToDeviceMap.containsKey(mcastAddress)) {
			Set<IDevice> memberSet = ConcurrentHashMap.newKeySet();
			memberSet.addAll(mcastToDeviceMap.get(mcastAddress));
			return memberSet;
		}
		return null;
	}
	
	public Set<IPAddress<?>> getGroups(IDevice device) {
		if (deviceToMcastMap.containsKey(device)) {
			Set<IPAddress<?>> groupSet = ConcurrentHashMap.newKeySet();
			groupSet.addAll(deviceToMcastMap.get(device));
			return groupSet;
		}
		return null;
	}
	
	public Set<IDevice> getAllMembers() {
		return deviceToMcastMap.keySet();
	}
	
	public Set<IPAddress<?>> getAllGroups() {
		return mcastToDeviceMap.keySet();
	}
	
	public boolean isMember(IDevice device) {
		return deviceToMcastMap.containsKey(device);
	}
	
	public boolean isGroup(IPAddress<?> mcastAddress) {
		return mcastToDeviceMap.containsKey(mcastAddress);
	}
	
	public void deleteGroup(IPAddress<?> mcastAddress) {
		if (mcastToDeviceMap.containsKey(mcastAddress)) {
			Set<IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
			for(IDevice device: memberSet) {
				remove(mcastAddress, device);
			}
		}
	}
	
	public void deleteMember(IDevice device) {
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