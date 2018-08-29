package net.floodlightcontroller.multicasting;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.devicemanager.IDevice;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Listener Interface for Multicasting
 * 
 */ 
public interface IMulticastListener {
	void ParticipantAdded(IPAddress<?> mcastAddress, IDevice device);
	
	void ParticipantRemoved(IPAddress<?> mcastAddress, IDevice device);
	
	void ParticipantGroupRemoved(IPAddress<?> mcastAddress);
	
	void ParticipantMemberRemoved(IDevice device);
	
	void ParticipantsReset();

	void ParticipantMemberUpdated(IDevice device);
}
