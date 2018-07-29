package net.floodlightcontroller.sessionmanager;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class SessionManagerResetResource extends ServerResource
{
	@Get("json")
	public String retrieve()
	{
		try
		{
	        ISessionManagerService ism = (ISessionManagerService)getContext().getAttributes().
	                get(ISessionManagerService.class.getCanonicalName());
	        ism.getParticipantTable().clearTable();
		}
		catch(Exception ex)
		{
			return "Fail\n" + ex;
		}
		return "Success";
	}
}
