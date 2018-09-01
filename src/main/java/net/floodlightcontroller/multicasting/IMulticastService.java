package net.floodlightcontroller.multicasting;

import java.util.Set;

import org.projectfloodlight.openflow.types.IPAddress;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.types.NodePortTuple;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * Service Interface for Multicasting
 * 
 */ 
public interface IMulticastService extends IFloodlightService {
	public void addParticipant(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap);
	
	public void removeParticipant(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap);
	
	public boolean hasParticipant(IPAddress<?> group, MacVlanPair intf);
	
	public Set<NodePortTuple> getParticipantAP(IPAddress<?> group, MacVlanPair intf);
	
	public Set<MacVlanPair> getParticipantIntfs(IPAddress<?> group);
	
	public Set<IPAddress<?>> getParticipantGroups(MacVlanPair intf);
	
	public Set<MacVlanPair> getAllParticipantIntfs();
	
	public Set<IPAddress<?>> getAllParticipantGroups();
	
	public boolean hasParticipantIntf(MacVlanPair intf);
	
	public boolean hasParticipantGroup(IPAddress<?> group);
	
	public void deleteParticipantGroup(IPAddress<?> group);
	
	public void deleteParticipantIntf(MacVlanPair intf);
	
	public void clearAllParticipants();
	
    public void addListener(IMulticastListener listener);

    public void removeListener(IMulticastListener listener);
}
