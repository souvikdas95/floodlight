package net.floodlightcontroller.multicasting.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.multicasting.IMulticastListener;
import net.floodlightcontroller.multicasting.IMulticastService;
import net.floodlightcontroller.topology.ITopologyService;

/**
 * @author Souvik Das (souvikdas95@yahoo.co.in)
 * 
 * MulticastManager is only a service gateway between ParticipantTable
 * in Multicast Service and the MulticastGroups & MulticastPaths in 
 * Topology Service.
 * 
 */
public class MulticastManager implements IFloodlightModule, IMulticastService, IDeviceListener {
	
    /**
     * Table contains multicast group membership information
     */
    private static ParticipantTable participantTable = new ParticipantTable();
	
	private static Set<IMulticastListener> multicastListeners = 
			new HashSet<IMulticastListener>();
	
	// private IFloodlightProviderService floodlightProviderService;
	private ITopologyService topologyService;
	private IDeviceService deviceService;
	
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
	    l.add(ITopologyService.class);
	    l.add(IDeviceService.class);
	    return l;
	}
	
	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		topologyService = context.getServiceImpl(ITopologyService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
	}
	
	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		deviceService.addListener(this);
	}
	
	@Override
	public void addParticipant(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap) {
		Set<NodePortTuple> attachmentPoints = participantTable.getAttachmentPoints(group, intf);
		if (attachmentPoints.isEmpty() || !attachmentPoints.contains(ap)) {
			for (IMulticastListener multicastingListener: multicastListeners) {
				multicastingListener.ParticipantAdded(group, intf, ap);
			}
			
			participantTable.add(group, intf, ap);
		}
	}

	@Override
	public void removeParticipant(IPAddress<?> group, MacVlanPair intf, NodePortTuple ap) {
		Set<NodePortTuple> attachmentPoints = participantTable.getAttachmentPoints(group, intf);
		if (attachmentPoints.isEmpty() && attachmentPoints.contains(ap)) {
			for (IMulticastListener multicastingListener: multicastListeners) {
				multicastingListener.ParticipantRemoved(group, intf, ap);
			}
			
			participantTable.remove(group, intf, ap);
		}
	}
	
	@Override
	public boolean hasParticipant(IPAddress<?> group, MacVlanPair intf) {
		return participantTable.contains(group, intf);
	}
	
	@Override
	public Set<NodePortTuple> getParticipantAP(IPAddress<?> group, MacVlanPair intf) {
		return participantTable.getAttachmentPoints(group, intf);
	}
	
	@Override
	public Set<MacVlanPair> getParticipantIntfs(IPAddress<?> group) {
		return participantTable.getIntfs(group);
	}

	@Override
	public Set<IPAddress<?>> getParticipantGroups(MacVlanPair intf) {
		return participantTable.getGroups(intf);
	}

	@Override
	public Set<MacVlanPair> getAllParticipantIntfs() {
		return participantTable.getAllIntfs();
	}

	@Override
	public Set<IPAddress<?>> getAllParticipantGroups() {
		return participantTable.getAllGroups();
	}

	@Override
	public boolean hasParticipantIntf(MacVlanPair intf) {
		return participantTable.hasIntf(intf);
	}

	@Override
	public boolean hasParticipantGroup(IPAddress<?> group) {
		return participantTable.hasGroup(group);
	}

	@Override
	public void deleteParticipantGroup(IPAddress<?> group) {
		if (participantTable.hasGroup(group)) {
			for (IMulticastListener multicastingListener: multicastListeners) {
				multicastingListener.ParticipantGroupRemoved(group);
			}
			
			participantTable.deleteGroup(group);
		}
	}

	@Override
	public void deleteParticipantIntf(MacVlanPair intf) {
		if (participantTable.hasIntf(intf)) {
			for (IMulticastListener multicastingListener: multicastListeners) {
				multicastingListener.ParticipantIntfRemoved(intf);
			}
			
			participantTable.deleteIntf(intf);
		}
	}

	@Override
	public void clearAllParticipants() {
		for (IMulticastListener multicastingListener: multicastListeners) {
			multicastingListener.ParticipantsReset();
		}
		
		participantTable.clearTable();
	}
	
	@Override
	public void addListener(IMulticastListener listener) {
		multicastListeners.add(listener);
	}
	
	@Override
	public void removeListener(IMulticastListener listener) {
		multicastListeners.remove(listener);
	}

	@Override
	public String getName() {
		return "multicasting";
	}

	@Override
	public boolean isCallbackOrderingPrereq(String type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(String type, String name) {
		return false;
	}

	@Override
	public void deviceAdded(IDevice device) {
		// nothing to do
	}

	@Override
	public void deviceRemoved(IDevice device) {
		MacAddress macAddress = device.getMACAddress();
		VlanVid[] vlanIds = device.getVlanId();
		for (VlanVid vlanId: vlanIds) {
			MacVlanPair intf = new MacVlanPair(macAddress, vlanId);
			Set<IPAddress<?>> groupSet = getParticipantGroups(intf);
			for (IPAddress<?> group: groupSet) {
				Set<NodePortTuple> memberAP = getParticipantAP(group, intf);
				for (NodePortTuple ap: memberAP) {
					removeParticipant(group, intf, ap);
				}
			}
		}
	}

	@Override
	public void deviceMoved(IDevice device) {
		/*
		 * Use archipelago of participant switches to determine, changes
		 * in attachmentPoints. The assumption is that a device can have 
		 * only 1 attachmentPoint per archipelago.
		 */
		NodePortTuple[] deviceAP = device.getAttachmentPoints();
		MacAddress macAddress = device.getMACAddress();
		VlanVid[] vlanIds = device.getVlanId();
		for (VlanVid vlanId: vlanIds) {
			MacVlanPair intf = new MacVlanPair(macAddress, vlanId);
			Set<IPAddress<?>> groupSet = getParticipantGroups(intf);
			for (IPAddress<?> group: groupSet) {
				// Reference attachmentPoints
				Set<NodePortTuple> memberAP = getParticipantAP(group, intf);

				// Classify Reference attachmentPoints based on Archipelago
				Map<DatapathId, NodePortTuple> bucket = 
						new HashMap<DatapathId, NodePortTuple>();
				for (NodePortTuple ap: memberAP) {
					DatapathId clusterId = topologyService.getClusterId(ap.getNodeId());
					bucket.put(clusterId, ap);
				}
				
				// Evaluate attachmentPoints based on Reference Archipelago and attachmentPoints
				for (NodePortTuple ap: deviceAP) {
					DatapathId clusterId = topologyService.getClusterId(ap.getNodeId());
					NodePortTuple refAp = bucket.get(clusterId);
					if (refAp != null) {
						if (!ap.equals(refAp)) {
							removeParticipant(group, intf, refAp);
							addParticipant(group, intf, ap);
						}
						bucket.remove(clusterId);
					}
				}
				
				// Remove remaining attachmentPoints
				for (NodePortTuple ap: bucket.values()) {
					removeParticipant(group, intf, ap);
				}
			}
		}
	}

	@Override
	public void deviceIPV4AddrChanged(IDevice device) {
		// nothing to do
	}

	@Override
	public void deviceIPV6AddrChanged(IDevice device) {
		// nothing to do
	}

	@Override
	public void deviceVlanChanged(IDevice device) {
		/*
		 * If any participant interface has the same Mac Address
		 * as that of the changed device, then its vlanId must 
		 * belong to that device. If not, then the corresponding
		 * interface is no longer valid and must be removed.
		 */
		MacAddress macAddress = device.getMACAddress();
		VlanVid[] vlanIds = device.getVlanId();
		Set<MacVlanPair> intfSet = getAllParticipantIntfs();
		for (MacVlanPair intf: intfSet) {
			MacAddress refMacAddress = intf.getMac();
			if (refMacAddress.equals(macAddress)) {
				VlanVid refVlanVid = intf.getVlan();
				boolean flag = false;
				for (VlanVid vlanId: vlanIds) {
					if (refVlanVid.equals(vlanId)) {
						flag = true;
						break;
					}
				}
				if (!flag) {
					deleteParticipantIntf(intf);
				}
			}
		}
		

	}
}