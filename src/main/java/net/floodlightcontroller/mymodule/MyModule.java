package net.floodlightcontroller.mymodule;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.VlanVid;
import org.restlet.engine.header.StringWriter;
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
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IPv6;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.statistics.SwitchPortBandwidth;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.util.OFMessageUtils;

public class MyModule implements IFloodlightModule, IOFMessageListener {

	protected static final Logger log = LoggerFactory.getLogger(MyModule.class);

	private Counter myCounter;
	private Gauge mySwBwRx;
	private Gauge mySwBwTx;
	
	private IDeviceService deviceService;
	private IFloodlightProviderService floodlightProviderService;
	private IRoutingService routingService;
	private IStatisticsService statisticsService;
	private IThreadPoolService threadPoolService;
	private ITopologyService topologyService;

	@Override
	public String getName() {
		return MyModule.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return (type.equals(OFType.PACKET_IN) &&
                (name.equals("linkdiscovery") ||
                        name.equals("topology") ||
                        name.equals("devicemanager")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return (type.equals(OFType.PACKET_IN) &&
        		name.equals("forwarding"));
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IDeviceService.class);
		l.add(IFloodlightProviderService.class);
		l.add(IRoutingService.class);
		l.add(IStatisticsService.class);
		l.add(IThreadPoolService.class);
		l.add(ITopologyService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		deviceService = context.getServiceImpl(IDeviceService.class);
		floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
		routingService = context.getServiceImpl(IRoutingService.class);
		statisticsService = context.getServiceImpl(IStatisticsService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		topologyService = context.getServiceImpl(ITopologyService.class);
		
		// Export Custom Metrics in Prometheus
		myCounter = Counter.build()
			     .name("myCounter")
			     .namespace("myNamespace") // optional
			     .subsystem("mySubsystem") // optional
			     .labelNames("myLabel1", "myLabel2") // optional
			     .help("myHelp")
			     .register();
		mySwBwRx = Gauge.build()
			     .name("mySwBwRx")
			     .labelNames("dpId", "port")
			     .help("swBwRx")
			     .register();
		mySwBwTx = Gauge.build()
			     .name("mySwBwTx")
			     .labelNames("dpId", "port")
			     .help("swBwTx")
			     .register();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// Add Listeners
		floodlightProviderService.addOFMessageListener(OFType.PACKET_IN, this);
		floodlightProviderService.addOFMessageListener(OFType.FLOW_REMOVED, this);
		
		// Export Floodlight JVM Metrics in Prometheus
		DefaultExports.initialize();
		
		// Export Bandwidth Switch RX/TX in Prometheus
		threadPoolService.getScheduledExecutor().execute(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Map<NodePortTuple, SwitchPortBandwidth> switchPortBandwidthMap = 
								statisticsService.getBandwidthConsumption();
						for (NodePortTuple npt: switchPortBandwidthMap.keySet()) {
							DatapathId dpId = npt.getNodeId();
							OFPort port = npt.getPortId();
							SwitchPortBandwidth switchPortBandwidth = switchPortBandwidthMap.get(npt);
							mySwBwRx.labels(dpId.toString(), port.toString()).set(switchPortBandwidth.getBitsPerSecondRx().getValue());
							mySwBwTx.labels(dpId.toString(), port.toString()).set(switchPortBandwidth.getBitsPerSecondTx().getValue());
						}
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
							StringWriter errors = new StringWriter();
							e.printStackTrace(new PrintWriter(errors));
							log.error(errors.toString());
						}
					} catch (Exception e) {
						StringWriter errors = new StringWriter();
						e.printStackTrace(new PrintWriter(errors));
						log.error(errors.toString());
					}
				}
			}
		});
		
		// Start Prometheus HTTP Export Server
		try {
			HTTPServer server = new HTTPServer(9249);
			log.info("Prometheus HTTP Server successfully started on Port 9249");
		} catch (IOException e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			log.error(errors.toString());
		}
		
		// TODO
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
			case PACKET_IN:
				return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
			case FLOW_REMOVED:
				return this.processFlowRemovedMessage(sw, (OFFlowRemoved) msg);
			case ERROR:
				log.info("received an error {} from switch {}", msg, sw);
				return Command.CONTINUE;
			default:
				log.error("received an unexpected message {} from switch {}", msg, sw);
				return Command.CONTINUE;
		}
	}

	private Command processFlowRemovedMessage(IOFSwitch sw, OFFlowRemoved msg) {
		// OFSwitch
		OFFactory factory = sw.getOFFactory();
		OFVersion swVersion = factory.getVersion();
		
		// OFMessage
		OFVersion msgVersion = msg.getVersion();

		// OFFlowRemoved
		Match match = msg.getMatch();

		// TODO
		return Command.CONTINUE;
	}

	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn msg, FloodlightContext cntx) {
		// Prometheus Counter Increment
		try {
			myCounter.labels("myValue1", "myValue2").inc();
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			log.error(errors.toString());
		}
		
		// OFSwitch
		OFFactory factory = sw.getOFFactory();
		OFVersion swVersion = factory.getVersion();
		
		// OFMessage
		OFVersion msgVersion = msg.getVersion();

		// OFPacketIn
		OFPort inPort = OFMessageUtils.getInPort(msg);
		Match match = msg.getMatch();
		OFVlanVidMatch matchVlanVid = OFMessageUtils.getVlan(msg);

		// L2
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		MacAddress sourceMACAddress = eth.getSourceMACAddress();
		MacAddress destinationMACAddress = eth.getDestinationMACAddress();
		EthType ethType = eth.getEtherType();
		VlanVid ethVlanID = VlanVid.ofVlan(eth.getVlanID());
		
		// L3		
		if (ethType.equals(EthType.ARP)) {
			// ARP
			ARP arp = (ARP) eth.getPayload();
			
			// TODO
		} else if (ethType.equals(EthType.IPv4)) {
			// IPv4
			IPv4 ipv4 = (IPv4) eth.getPayload();
			IpProtocol ipProtocol = ipv4.getProtocol();
			IPv4Address sourceIpv4Address = ipv4.getSourceAddress();
			IPv4Address destinationIpv4Address = ipv4.getDestinationAddress();
			
			if (ipProtocol.equals(IpProtocol.TCP)) {
				TCP tcp = (TCP) ipv4.getPayload();
				
				// TODO
			} else if (ipProtocol.equals(IpProtocol.UDP)) {
				UDP udp = (UDP) ipv4.getPayload();
				
				// TODO
			} else if (ipProtocol.equals(IpProtocol.ICMP)) {
				ICMP icmp = (ICMP) ipv4.getPayload();
				
				// TODO
			}
		} else if (ethType.equals(EthType.IPv6)) {
			// IPv6
			IPv6 ipv6 = (IPv6) eth.getPayload();
			
			// TODO
		}

		// TODO
		return Command.CONTINUE;
	}
}
