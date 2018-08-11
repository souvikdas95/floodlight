package net.floodlightcontroller.util;

import java.math.BigInteger;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPAddress;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;

public class MulticastUtils {
    /*
     * Generates MgId from Mcast IPAddress (For internal use only)
     */
	public static BigInteger MgIdFromMcastIP(IPAddress<?> mcastIp) {
		return new BigInteger(mcastIp.getBytes());
	}
	
	/*
	 * Generates Mcast IPAddress from MgId (For internal use only)
	 */
	public static IPAddress<?> McastIPFromMgId(BigInteger mgId) {
		byte[] bMgId = mgId.toByteArray();
		byte[] bIp = new byte[bMgId.length];
		if (bIp.length == 4) {	/* IPv4 Multicast */
			return IPv4Address.of(bIp);
		}
		if (bIp.length == 16) { /* IPv6 Multicast */
			return IPv6Address.of(bIp);
		}
		return null;
	}
}