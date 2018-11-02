package net.floodlightcontroller.sessionmanager;

import java.util.Set;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import net.floodlightcontroller.core.types.MacVlanPair;
import net.floodlightcontroller.multicasting.IMulticastService;
import net.floodlightcontroller.multicasting.internal.ParticipantGroupAddress;

public class SessionManagerParticipantsResource extends ServerResource
{
	@Get("json")
	public String retrieve()
	{
        try {
			IMulticastService ims = (IMulticastService)getContext().getAttributes().
	                get(IMulticastService.class.getCanonicalName());
	        Set<ParticipantGroupAddress> participantGroupAddresses = ims.getAllParticipantGroupAddresses();
	        StringBuilder sb = new StringBuilder();
	        for (ParticipantGroupAddress participantGroupAddress: participantGroupAddresses) {
	        	sb.append("(" + participantGroupAddress + "): \n");
	        	Set<MacVlanPair> participantIntfs = ims.getParticipantIntfs(participantGroupAddress);
	        	for (MacVlanPair participantIntf: participantIntfs) {
	        		sb.append("\t" + participantIntf + "\n");
	        	}
	        }
	        return sb.toString();
        } catch(Exception ex) {
        	return "Fail\n" + ex;
        }
	}
}
