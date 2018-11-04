package net.floodlightcontroller.sessionmanager;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.floodlightcontroller.multicasting.internal.ParticipantGroupAddress;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.MulticastPath;

public class SessionManagerPathStats extends ServerResource
{
	@Get("json")
	public String retrieve()
	{
		try
		{
			IRoutingService irs = (IRoutingService)getContext().getAttributes().
	                get(IRoutingService.class.getCanonicalName());
			
			String szValue = null;
			
			// Source Switch DatapathId
			szValue = getQuery().getValues("srcSwId");
			DatapathId srcSwId = null;
			if (szValue != null) {
				srcSwId = DatapathId.of(szValue);
			}
			else {
				return "{}";
			}
			
			// Destination MacAddress
			szValue = getQuery().getValues("dstMacAddress");
			MacAddress macAddress = null;
			if (szValue != null) {
				macAddress = MacAddress.of(szValue);
			}
			
			// Destination VlanVid
			szValue = getQuery().getValues("dstVlanVid");
			VlanVid vlanVid = null;
			if (szValue != null) {
				vlanVid = VlanVid.ofVlan(Integer.parseInt(szValue));
			}
			
			// Destination IPAddress
			szValue = getQuery().getValues("dstIPAddress");
			IPAddress<?> ipAddress = null;
			if (szValue != null) {
				ipAddress = IPAddress.of(szValue);
			}
			
			// Destination TransportPort
			szValue = getQuery().getValues("dstTransportPort");
			TransportPort port = null;
			if (szValue != null) {
				port = TransportPort.of(Integer.parseInt(szValue));
			}
			
			ParticipantGroupAddress pgAddress = new ParticipantGroupAddress(
					macAddress,
					vlanVid,
					ipAddress,
					port
					);
			
			MulticastPath mPath = irs.getMulticastPath(srcSwId, pgAddress);
			
			if (! mPath.isEmpty()) {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.createObjectNode();
				((ObjectNode) rootNode).put("HopCount", mPath.getHopCount());
				((ObjectNode) rootNode).put("Latency", mPath.getLatency().getValue());
				((ObjectNode) rootNode).put("Cost", mPath.getCost());
				return mapper.writeValueAsString(rootNode);
			}
		}
		catch(Exception ex)
		{
			return "{\"Exception\" : \"" + ex + "\"}";
		}
		return "{}";
	}
}
