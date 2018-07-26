package net.floodlightcontroller.sessionmanager;

import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.python.google.common.collect.ImmutableList;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.core.types.JsonObjectWrapper;
import net.floodlightcontroller.devicemanager.IDevice;

public class SessionManagerParticipantsResource extends ServerResource
{
	@Get("json")
	public Object retrieve()
	{
		try
		{
	        ISessionManagerService ism = (ISessionManagerService)getContext().getAttributes().
	                get(ISessionManagerService.class.getCanonicalName());
	        Map<IPv4Address, Collection<? extends IDevice>> participantsMap = ism.getParticipantsMap();
	        if (participantsMap != null)
	        {
	            return JsonObjectWrapper.of(participantsMap);
	        }
	        return JsonObjectWrapper.of(ImmutableList.of());
		}
		catch(Exception ex)
		{
			return JsonObjectWrapper.of(ImmutableList.of());
		}
	}
}
