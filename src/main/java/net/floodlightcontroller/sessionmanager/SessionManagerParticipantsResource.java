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
	        sb.append('{');
	        for (ParticipantGroupAddress participantGroupAddress: participantGroupAddresses) {
	        	sb.append("\"" + participantGroupAddress + "\" : [");
	        	Set<MacVlanPair> participantIntfs = ims.getParticipantIntfs(participantGroupAddress);
	        	for (MacVlanPair participantIntf: participantIntfs) {
	        		sb.append("\"" + ims.getParticipantAP(participantGroupAddress, participantIntf).iterator().next() + " : " + participantIntf + "\",");
	        	}
	        	if (participantIntfs.isEmpty()) {
	        		sb.append(']');
	        	}
	        	else {
	        		sb.setCharAt(sb.length() - 1, ']');
	        	}
	        	sb.append(',');
	        }
        	if (participantGroupAddresses.isEmpty()) {
        		sb.append('}');
        	}
        	else {
        		sb.setCharAt(sb.length() - 1, '}');
        	}
	        return sb.toString();
        } catch(Exception ex) {
        	return "Fail\n" + ex;
        }
	}
}
