package net.floodlightcontroller.sessionmanager;

import org.projectfloodlight.openflow.types.IPv4Address;

import net.floodlightcontroller.packet.BasePacket;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.PacketParsingException;

/*
 * Session Announcement Protocol
 * (Dummy - For Experimental Purpose only)
 */
public class SAP extends BasePacket
{
	protected byte version;
	protected IPv4Address sourceAddress;
	protected int messageID;
	
	// Mighty Constructor
	public SAP(IPacket packet) throws PacketParsingException
	{
		byte[] packetBytes = packet.serialize();
		deserialize(packetBytes, 0, packetBytes.length);
	}
	
	/**
	 * @return the version
	 */
	public byte getVersion()
	{
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public SAP setVersion(byte version)
	{
		this.version = version;
		return this;
	}
	
	/**
	 * @return the sourceAddress
	 */
	public IPv4Address getSourceAddress()
	{
		return sourceAddress;
	}
	
	/**
	 * @param sourceAddress the sourceAddress to set
	 */
	public SAP setSourceAddress(IPv4Address sourceAddress)
	{
		this.sourceAddress = sourceAddress;
		return this;
	}
	
	/**
	 * @return the messageID
	 */
	public Integer getMessageID()
	{
		return messageID;
	}
	
	/**
	 * @param messageID the messageID to set
	 */
	public SAP setMessageID(Integer messageID)
	{
		this.messageID = messageID;
		return this;
	}
	
	@Override
	public byte[] serialize()
	{
		return null;
	}

	@Override
	public IPacket deserialize(byte[] data, int offset, int length) throws PacketParsingException
	{
		String[] szArrData = new String(data, offset, length).split("\\$", 2);
		String[] szHeaderData = szArrData[0].split(",", 3);
		try
		{			
			// Extract Version
			String[] szArrVersion = szHeaderData[0].split("=", 2);
			if (!szArrVersion[0].equals("v"))
			{
				return null;
			}
			setVersion((byte)Integer.parseInt(szArrVersion[1]));
			
			// Extract SourceAddress
			String[] szArrSourceAddress = szHeaderData[1].split("=", 2);
			if (!szArrSourceAddress[0].equals("srcIP"))
			{
				return null;
			}
			setSourceAddress(IPv4Address.of(szArrSourceAddress[1]));
			
			// Extract MessageID
			String[] szArrMessageID = szHeaderData[2].split("=", 2);
			if (!szArrMessageID[0].equals("msgID"))
			{
				return null;
			}
			setMessageID(Integer.parseInt(szArrMessageID[1]));
			
			// Extract Payload (Optional)
			if (szArrData.length < 2 || szArrData[1].isEmpty())
			{
				setPayload(new Data());
			}
			else
			{
				setPayload(new Data(szArrData[1].getBytes()));
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
		
		return this;
	}

}
