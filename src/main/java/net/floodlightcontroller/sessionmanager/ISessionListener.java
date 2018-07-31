package net.floodlightcontroller.sessionmanager;

import org.projectfloodlight.openflow.types.IPv4Address;
import net.floodlightcontroller.devicemanager.IDevice;

public interface ISessionListener {
	void ParticipantAdded(IPv4Address mcastAddress, IDevice device);
	void ParticipantRemoved(IPv4Address mcastAddress, IDevice device);
}