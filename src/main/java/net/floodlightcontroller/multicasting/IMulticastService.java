package net.floodlightcontroller.multicasting;

import java.util.Set;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Service Interface for Multicasting
 * 
 */ 
public interface IMulticastService extends IFloodlightService {
	public void addParticipant(IPAddress<?> mcastAddress, IDevice device);
	
	public void removeParticipant(IPAddress<?> mcastAddress, IDevice device);
	
	public boolean hasParticipant(IPAddress<?> mcastAddress, IDevice device);
	
	public Set<IDevice> getParticipantMembers(IPAddress<?> mcastAddress);
	
	public Set<IPAddress<?>> getParticipantGroups(IDevice device);
	
	public Set<IDevice> getAllParticipantMembers();
	
	public Set<IPAddress<?>> getAllParticipantGroups();
	
	public boolean isParticipantMember(IDevice device);
	
	public boolean isParticipantGroup(IPAddress<?> mcastAddress);
	
	public void deleteParticipantGroup(IPAddress<?> mcastAddress);
	
	public void deleteParticipantMember(IDevice device);
	
	public void clearAllParticipants();
	
    public void addListener(IMulticastListener listener);

    public void removeListener(IMulticastListener listener);
}
