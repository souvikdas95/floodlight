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
	public void addParticipant(IPAddress<?> groupAddress, MacVlanPair intf, NodePortTuple ap);
	
	public void removeParticipant(IPAddress<?> groupAddress, MacVlanPair intf, NodePortTuple ap);
	
	public boolean hasParticipant(IPAddress<?> groupAddress, MacVlanPair intf);
	
	public Set<NodePortTuple> getParticipantAP(IPAddress<?> groupAddress, MacVlanPair intf);
	
	public Set<MacVlanPair> getParticipantIntfs(IPAddress<?> groupAddress);
	
	public Set<IPAddress<?>> getParticipantGroupAddresses(MacVlanPair intf);
	
	public Set<MacVlanPair> getAllParticipantIntfs();
	
	public Set<IPAddress<?>> getAllParticipantGroupAddresses();
	
	public boolean hasParticipantIntf(MacVlanPair intf);
	
	public boolean hasParticipantGroupAddress(IPAddress<?> groupAddress);
	
	public void deleteParticipantGroupAddress(IPAddress<?> groupAddress);
	
	public void deleteParticipantIntf(MacVlanPair intf);
	
	public void clearAllParticipants();
	
    public void addListener(IMulticastListener listener);

    public void removeListener(IMulticastListener listener);
}
