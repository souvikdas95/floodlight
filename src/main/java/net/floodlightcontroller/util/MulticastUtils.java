package net.floodlightcontroller.util;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;

public class MulticastUtils {
    /*
     * Generates Dpid from Mcast IPv4Address (Experimental only)
     */
	public static DatapathId DpidFromMcastIP(IPv4Address mcastIp) {
		byte[] bDpid = new byte[8];
		byte[] bIp = mcastIp.getBytes();
		bDpid[0] = (byte) 0xFF;
		bDpid[1] = (byte) 0xFF;
		bDpid[2] = (byte) 0xFF;
		bDpid[3] = (byte) 0xFF;
		bDpid[4] = (byte) (bIp[0] | 0xF0);
		bDpid[5] = bIp[1];
		bDpid[6] = bIp[2];
		bDpid[7] = bIp[3];
		return DatapathId.of(bDpid);
	}
	
	/*
	 * Generates Mcast IPv4Address from Dpid (Experimental only)
	 */
	public static IPv4Address McastIPFromDpid(DatapathId dpid) {
		byte[] bIp = new byte[4];
		byte[] bDpid = dpid.getBytes();
		bIp[0] = (byte) (bDpid[4] & 0xEF);
		bIp[1] = bDpid[5];
		bIp[2] = bDpid[6];
		bIp[3] = bDpid[7];
		return IPv4Address.of(bIp);
	}
}
