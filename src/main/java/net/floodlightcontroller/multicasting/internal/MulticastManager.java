package net.floodlightcontroller.multicasting.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.multicasting.IMulticastListener;
import net.floodlightcontroller.multicasting.IMulticastService;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * MulticastManager is only a service gateway between ParticipantTable
 * in Multicast Service and the MulticastGroups & MulticastPaths in 
 * Topology Service
 */
public class MulticastManager implements IFloodlightModule, IMulticastService {
	
    /**
     * Table contains multicast group membership information
     */
    protected static ParticipantTable participantTable = new ParticipantTable();
	
	protected Set<IMulticastListener> multicastingListeners;
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IMulticastService.class);
		return l;
	}
	
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>,  IFloodlightService> m = 
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IMulticastService.class, this);
		return m;
	}
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(IFloodlightProviderService.class);
	    return l;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		multicastingListeners = new HashSet<IMulticastListener>();
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
	}
	
	@Override
	public void addParticipant(IPAddress<?> mcastAddress, IDevice device) {
		// Inform listeners
		for (IMulticastListener multicastingListener: multicastingListeners) {
			multicastingListener.ParticipantAdded(mcastAddress, device);
		}
		
		participantTable.add(mcastAddress, device);
	}

	@Override
	public void removeParticipant(IPAddress<?> mcastAddress, IDevice device) {
		// Inform listeners
		for (IMulticastListener multicastingListener: multicastingListeners) {
			multicastingListener.ParticipantRemoved(mcastAddress, device);
		}
		
		participantTable.remove(mcastAddress, device);
	}

	@Override
	public boolean hasParticipant(IPAddress<?> mcastAddress, IDevice device) {
		return participantTable.contains(mcastAddress, device);
	}

	@Override
	public Set<IDevice> getParticipantMembers(IPAddress<?> mcastAddress) {
		return participantTable.getMembers(mcastAddress);
	}

	@Override
	public Set<IPAddress<?>> getParticipantGroups(IDevice device) {
		return participantTable.getGroups(device);
	}

	@Override
	public Set<IDevice> getAllParticipantMembers() {
		return participantTable.getAllMembers();
	}

	@Override
	public Set<IPAddress<?>> getAllParticipantGroups() {
		return participantTable.getAllGroups();
	}

	@Override
	public boolean isParticipantMember(IDevice device) {
		return participantTable.isMember(device);
	}

	@Override
	public boolean isParticipantGroup(IPAddress<?> mcastAddress) {
		return participantTable.isGroup(mcastAddress);
	}

	@Override
	public void deleteParticipantGroup(IPAddress<?> mcastAddress) {
		// Inform listeners
		for (IMulticastListener multicastingListener: multicastingListeners) {
			multicastingListener.ParticipantGroupRemoved(mcastAddress);
		}
		
		participantTable.deleteGroup(mcastAddress);
	}

	@Override
	public void deleteParticipantMember(IDevice device) {
		// Inform listeners
		for (IMulticastListener multicastingListener: multicastingListeners) {
			multicastingListener.ParticipantMemberRemoved(device);
		}
		
		participantTable.deleteMember(device);
	}

	@Override
	public void clearAllParticipants() {
		// Inform listeners
		for (IMulticastListener multicastingListener: multicastingListeners) {
			multicastingListener.ParticipantsReset();
		}
		
		participantTable.clearTable();
	}
	
	@Override
	public void addListener(IMulticastListener listener) {
		multicastingListeners.add(listener);
	}
	
	@Override
	public void removeListener(IMulticastListener listener) {
		multicastingListeners.remove(listener);
	}
}