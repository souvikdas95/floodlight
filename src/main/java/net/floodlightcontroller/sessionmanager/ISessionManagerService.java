package net.floodlightcontroller.sessionmanager;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface ISessionManagerService extends IFloodlightService
{
	public ParticipantTable getParticipantTable();
	
    public void addListener(ISessionListener listener);

    public void removeListener(ISessionListener listener);
}
