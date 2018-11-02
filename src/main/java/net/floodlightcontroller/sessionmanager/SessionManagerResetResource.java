package net.floodlightcontroller.sessionmanager;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.multicasting.IMulticastService;

public class SessionManagerResetResource extends ServerResource
{
	@Get("json")
	public String retrieve()
	{
		try
		{
	        IMulticastService ims = (IMulticastService)getContext().getAttributes().
	                get(IMulticastService.class.getCanonicalName());
	        ims.clearAllParticipants();
		}
		catch(Exception ex)
		{
			return "Fail\n" + ex;
		}
		return "Success";
	}
}
