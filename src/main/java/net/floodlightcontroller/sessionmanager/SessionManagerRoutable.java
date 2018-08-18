package net.floodlightcontroller.sessionmanager;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class SessionManagerRoutable implements RestletRoutable
{
	@Override
	public Restlet getRestlet(Context context)
	{
		Router router = new Router(context);
        router.attach("/reset", SessionManagerResetResource.class);
        router.attach("/participants", SessionManagerParticipantsResource.class);
        return router;
	}

	@Override
	public String basePath()
	{
		return "/wm/sessionmanager";
	}
}
