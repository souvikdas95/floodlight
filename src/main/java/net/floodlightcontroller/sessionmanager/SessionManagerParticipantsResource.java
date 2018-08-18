package net.floodlightcontroller.sessionmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.projectfloodlight.openflow.types.IPAddress;
import org.python.google.common.collect.ImmutableList;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.core.types.JsonObjectWrapper;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.multicasting.IMulticastService;

public class SessionManagerParticipantsResource extends ServerResource
{
	@Get("json")
	public Object retrieve()
	{
		try
		{
	        IMulticastService ims = (IMulticastService)getContext().getAttributes().
	                get(IMulticastService.class.getCanonicalName());
	        Set<IPAddress<?>> participantGroups = ims.getAllParticipantGroups();
	        Map<IPAddress<?>, Set<IDevice>> participantMap = new HashMap<IPAddress<?>, Set<IDevice>>();
	        for (IPAddress<?> participantGroup: participantGroups) {
	        	Set<IDevice> participantMembers = ims.getParticipantMembers(participantGroup);
	        	IPAddress<?> key = participantGroup;
	        	Set<IDevice> value = participantMembers;
	        	participantMap.put(key, value);
	        }
	        return JsonObjectWrapper.of(participantMap);
		}
		catch(Exception ex)
		{
			return JsonObjectWrapper.of(ImmutableList.of());
		}
	}
}
