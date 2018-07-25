package net.floodlightcontroller.sessionmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;
import net.sourceforge.jsdp.SDPFactory;
import net.sourceforge.jsdp.SDPParseException;
import net.sourceforge.jsdp.SessionDescription;

public class SDP extends BasePacket
{
	protected SessionDescription sessionDescription;
	
	/**
	 * @return the sessionDescription
	 */
	public SessionDescription getSessionDescription()
	{
		return sessionDescription;
	}

	/**
	 * @param sessionDescription the sessionDescription to set
	 */
	public SDP setSessionDescription(SessionDescription sessionDescription)
	{
		this.sessionDescription = sessionDescription;
		return this;
	}
	
	@Override
	public byte[] serialize()
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try
		{
			SDPFactory.encode(sessionDescription, byteArrayOutputStream);
		}
		catch (IOException e)
		{
			return null;
		}
		return byteArrayOutputStream.toByteArray();
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException
	{
		String szData = new String(data, offset, length);
		try
		{
			sessionDescription = SDPFactory.parseSessionDescription(szData);
		}
		catch(SDPParseException ex)
		{
			return null;
		}
		return this;
	}

}
