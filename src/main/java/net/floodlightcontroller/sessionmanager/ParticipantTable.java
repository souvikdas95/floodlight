package net.floodlightcontroller.sessionmanager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.types.IPv4Address;

import net.floodlightcontroller.devicemanager.IDevice;

public class ParticipantTable implements Serializable
{	
	/**
	 * Used during compilation
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<IPv4Address, Collection<? extends IDevice>> mcastToDeviceMap;
	private transient Map<IDevice, Set<IPv4Address>> deviceToMcastMap;
	
	private transient SessionManager parent;
	
	public ParticipantTable(SessionManager sessionManager)
	{
		parent = sessionManager;
		mcastToDeviceMap = new ConcurrentHashMap<IPv4Address, Collection<? extends IDevice>>();
		deviceToMcastMap = new ConcurrentHashMap<IDevice, Set<IPv4Address>>();
	}

	/*
	 * Return an Immutable Map
	 */
	public Map<IPv4Address, Collection<? extends IDevice>> getMcastToDeviceMap()
	{
		return Collections.unmodifiableMap(mcastToDeviceMap);
	}
	
	@Override
	public String toString()
	{
		return mcastToDeviceMap.toString();
	}
	
	public void add(IPv4Address mcastAddress, IDevice device, boolean notify)
	{
		boolean toNotify = false;
		
		// Add to Member Set
		if (!mcastToDeviceMap.containsKey(mcastAddress))
		{
			mcastToDeviceMap.put(mcastAddress, new HashSet<IDevice>());
		}
		@SuppressWarnings("unchecked")
		Collection<IDevice> memberSet = (Collection<IDevice>) mcastToDeviceMap.get(mcastAddress);
		if (!memberSet.contains(device))
		{
			memberSet.add(device);
			toNotify = true;
		}
		
		// Add to Group Set
		if (!deviceToMcastMap.containsKey(device))
		{
			deviceToMcastMap.put(device, new HashSet<IPv4Address>());
		}
		Set<IPv4Address> groupSet = deviceToMcastMap.get(device);
		if (!groupSet.contains(mcastAddress))
		{
			groupSet.add(mcastAddress);
			toNotify = true;
		}
		
		if (toNotify && notify) {
			// Inform Listeners
			for (ISessionListener sessionListener: parent.sessionListeners) {
				sessionListener.ParticipantAdded(mcastAddress, device);
			}
		}
	}
	
	public void remove(IPv4Address mcastAddress, IDevice device, boolean notify)
	{
		boolean toNotify = false;
		
		// Remove from Member Set
		if (mcastToDeviceMap.containsKey(mcastAddress))
		{
			Collection<? extends IDevice> memberSet  = mcastToDeviceMap.get(mcastAddress);
			if (memberSet.contains(device))
			{
				memberSet.remove(device);
				if (memberSet.isEmpty())
				{
					mcastToDeviceMap.remove(mcastAddress);
				}
				toNotify = true;
			}
		}
		
		// Remove from Group Set
		if (deviceToMcastMap.containsKey(device))
		{
			Set<IPv4Address> groupSet = deviceToMcastMap.get(device);
			if (groupSet.contains(mcastAddress))
			{
				groupSet.remove(mcastAddress);
				if (groupSet.isEmpty())
				{
					deviceToMcastMap.remove(device);
				}
				toNotify = true;
			}
		}
		
		if (toNotify && notify) {
			// Inform Listeners
			for (ISessionListener sessionListener: parent.sessionListeners) {
				sessionListener.ParticipantRemoved(mcastAddress, device);
			}
		}
	}

	public boolean contains(IPv4Address mcastAddress, IDevice device)
	{
		if (mcastToDeviceMap.containsKey(mcastAddress) && deviceToMcastMap.containsKey(device))
		{
			return true;
		}
		
		return false;
	}
	
	public Set<IDevice> getMembers(IPv4Address mcastAddress)
	{
		if (mcastToDeviceMap.containsKey(mcastAddress))
		{
			return new HashSet<IDevice>(mcastToDeviceMap.get(mcastAddress));
		}
		
		return null;
	}
	
	public Set<IPv4Address> getGroups(IDevice device)
	{
		if (deviceToMcastMap.containsKey(device))
		{
			return new HashSet<IPv4Address>(deviceToMcastMap.get(device));
		}
		
		return null;
	}
	
	public Set<IDevice> getAllMembers()
	{
		return deviceToMcastMap.keySet();
	}
	
	public Set<IPv4Address> getAllGroups()
	{
		return mcastToDeviceMap.keySet();
	}
	
	public boolean isMember(IDevice device)
	{
		return deviceToMcastMap.containsKey(device);
	}
	
	public boolean isGroup(IPv4Address mcastAddress)
	{
		return mcastToDeviceMap.containsKey(mcastAddress);
	}
	
	public void deleteGroup(IPv4Address mcastAddress, boolean notify)
	{
		if (mcastToDeviceMap.containsKey(mcastAddress))
		{
			Collection<? extends IDevice> memberSet = mcastToDeviceMap.get(mcastAddress);
			
			for(IDevice device: memberSet)
			{
				remove(mcastAddress, device, notify);
			}
		}
	}
	
	public void deleteMember(IDevice device, boolean notify)
	{
		if (deviceToMcastMap.containsKey(device))
		{
			Set<IPv4Address> groupSet = deviceToMcastMap.get(device);
			
			for(IPv4Address mcastAddress: groupSet)
			{
				remove(mcastAddress, device, notify);
			}
		}
	}
	
	public void clearTable(boolean notify)
	{
		// Create a copy
		Map<IPv4Address, Collection<? extends IDevice>> mcastToDeviceMapCopy = null;
		if (notify) {
			mcastToDeviceMapCopy = new ConcurrentHashMap<IPv4Address, Collection<? extends IDevice>>();
		}
		
		// Clear
		mcastToDeviceMap.clear();
		deviceToMcastMap.clear();
		
		// Inform Listeners
		if (notify && mcastToDeviceMapCopy != null) {
			Set<Map.Entry<IPv4Address, Collection<? extends IDevice>>> entries = mcastToDeviceMapCopy.entrySet();
			for (Map.Entry<IPv4Address, Collection<? extends IDevice>> entry: entries) {
				IPv4Address mcastAddress = entry.getKey();
				Collection<? extends IDevice> memberSet = entry.getValue();
				for (IDevice device: memberSet) {
					for (ISessionListener sessionListener: parent.sessionListeners) {
						sessionListener.ParticipantRemoved(mcastAddress, device);
					}
				}
			}
		}
	}
}