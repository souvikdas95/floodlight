package net.floodlightcontroller.sessionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.restserver.IRestApiService;
import net.sourceforge.jsdp.SessionDescription;

public class SessionManager implements IOFMessageListener, IFloodlightModule, ISessionManagerService
{
	protected static Logger logger;
	
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApiService;
	
	protected Map<IPv4Address, Collection<? extends IDevice>> participantsMap;
	
	@Override
	public String getName()
	{
		return "sessionmanager";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<IPv4Address, Collection<? extends IDevice>> getParticipantsMap()
	{
		return participantsMap;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices()
	{
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(ISessionManagerService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
	{
		Map<Class<? extends IFloodlightService>,  IFloodlightService> m = 
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(ISessionManagerService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
	{
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException
	{
		logger = LoggerFactory.getLogger(SessionManager.class);
		floodlightProvider = (IFloodlightProviderService) context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = (IRestApiService) context.getServiceImpl(IRestApiService.class);
		participantsMap = new HashMap<IPv4Address, Collection<? extends IDevice>>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException
	{
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiService.addRestletRoutable(new SessionManagerRoutable());
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx)
	{
		switch (msg.getType())
		{
			case PACKET_IN:
				return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
			default:
				return Command.CONTINUE;
		}
	}

	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx)
	{
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (eth.getEtherType() == EthType.IPv4)
		{
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address destAddress = ip.getDestinationAddress();
			if (ip.getProtocol() == IpProtocol.UDP &&
					destAddress != null &&
					destAddress != IPv4Address.NONE &&
					!destAddress.isBroadcast() &&
					!destAddress.isMulticast() &&
					!destAddress.isLoopback() &&
					!destAddress.isLinkLocal() &&
					!destAddress.isCidrMask())
			{
				UDP udp = (UDP) ip.getPayload();
				try
				{
					IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
					SAP sap = new SAP(udp.getPayload());
					SDP sdp = new SDP(sap.getPayload());
					SessionDescription sessionDescription = sdp.getSessionDescription();
					IPv4Address mcastAddress = IPv4Address.of(sessionDescription.getConnection()
							.getAddress().split("/", 2)[0]);
					if (participantsMap.containsKey(mcastAddress))
					{
						@SuppressWarnings("unchecked")
						HashSet<IDevice> participantsSet = (HashSet<IDevice>) participantsMap.get(mcastAddress);
						participantsSet.add(dstDevice);
					}
					else
					{
						HashSet<IDevice> participantsSet = new HashSet<IDevice>();
						participantsSet.add(dstDevice);
						participantsMap.put(mcastAddress, participantsSet);
					}
				}
				catch (Exception ex)
				{
					return Command.CONTINUE;
				}
			}
		}
		return Command.CONTINUE;
	}
}
