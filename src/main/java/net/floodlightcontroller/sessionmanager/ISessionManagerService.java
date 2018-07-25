package net.floodlightcontroller.sessionmanager;

import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;

public interface ISessionManagerService extends IFloodlightService
{
	public Map<IPv4Address, Collection<? extends IDevice>> getParticipantsMap();
}
