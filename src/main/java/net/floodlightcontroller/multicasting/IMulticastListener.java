package net.floodlightcontroller.multicasting;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.types.NodePortTuple;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Listener Interface for Multicasting
 * 
 */ 
public interface IMulticastListener {
	void ParticipantAdded(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap);
	
	void ParticipantRemoved(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap);
	
	void ParticipantsReset();
}
