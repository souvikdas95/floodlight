package net.floodlightcontroller.sessionmanager;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ISessionManagerService extends IFloodlightService
{
	public ParticipantTable getParticipantTable();
}
