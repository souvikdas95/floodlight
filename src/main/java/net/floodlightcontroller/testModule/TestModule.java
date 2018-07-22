package net.floodlightcontroller.testModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Formatter;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
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
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.util.OFMessageUtils;

public class TestModule implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	
	@Override
	public String getName() {
		return TestModule.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> ret =
		        new ArrayList<Class<? extends IFloodlightService>>();
	    ret.add(IFloodlightProviderService.class);
	    return ret;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    logger = LoggerFactory.getLogger(TestModule.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		Ethernet eth =
                IFloodlightProviderService.bcStore.get(cntx,
                                            IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
 
        EthType ethType = eth.getEtherType();
        if (ethType != EthType.IPv4)
        	return Command.CONTINUE;
		
		MacAddress sourceMACAddress = eth.getSourceMACAddress();
        MacAddress destinationMACAddress = eth.getDestinationMACAddress();
        IPv4 ipv4 = (IPv4) eth.getPayload();
        IPv4Address sourceAddress = ipv4.getSourceAddress();
        IPv4Address destinationAddress = ipv4.getDestinationAddress();
        OFPort inPort = OFMessageUtils.getInPort((OFPacketIn)msg);
        
        String log = String.format("PACKET_IN: Source {%s, %s}, Destionation {%s, %s}. Port {%d}, Switch {%s}",
        		sourceMACAddress.toString(),
        		sourceAddress.toString(),
        		destinationMACAddress.toString(),
        		destinationAddress.toString(),
        		inPort.getPortNumber(),
        		sw.getId().toString());
        logger.info(log);
        
        return Command.CONTINUE;
	}

}
