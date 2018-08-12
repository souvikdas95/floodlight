package net.floodlightcontroller.multicasting;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.devicemanager.IDevice;

public interface IMulticastListener {
	void ParticipantAdded(IPAddress<?> mcastAddress, IDevice device);
	
	void ParticipantRemoved(IPAddress<?> mcastAddress, IDevice device);
	
	void ParticipantGroupRemoved(IPAddress<?> mcastAddress);
	
	void ParticipantMemberRemoved(IDevice device);
	
	void ParticipantsReset();
}