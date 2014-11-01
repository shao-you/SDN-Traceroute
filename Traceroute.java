package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.List;
import java.util.Set;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.IEntityClassListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;

public class Traceroute implements IOFMessageListener, IFloodlightModule, IOFSwitchListener, ITopologyListener, IEntityClassListener {
 
	protected IFloodlightProviderService floodlightProvider;
	protected ICounterStoreService counterStore;
	protected IDeviceService deviceManager;
	protected IRoutingService routingEngine;
	protected ILinkDiscoveryService linkManager;
	protected ITopologyService topology;
	
	protected Logger logger;
	protected boolean flag = true;
	Map<Long, swInfo>color;
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Traceroute";
	}
 
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}
 
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));//比forwarding先處理pkt_in
		//return false;
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
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(ICounterStoreService.class);
		    l.add(IRoutingService.class);
	        l.add(IDeviceService.class);
	        l.add(ILinkDiscoveryService.class);	
	        l.add(ITopologyService.class);	        
		return l;
		// TODO Auto-generated method stub
	}
 
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		this.counterStore = context.getServiceImpl(ICounterStoreService.class);
		this.routingEngine = context.getServiceImpl(IRoutingService.class);
		this.deviceManager = context.getServiceImpl(IDeviceService.class);
		this.linkManager = context.getServiceImpl(ILinkDiscoveryService.class);
		this.topology = context.getServiceImpl(ITopologyService.class);
		this.logger = LoggerFactory.getLogger(Traceroute.class);
		// TODO Auto-generated method stub
	}
 
	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);//註冊成為其他Service的Listener
		floodlightProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        floodlightProvider.addOFSwitchListener(this);
        topology.addListener(this);
		// TODO Auto-generated method stub
	}
 
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		//if(flag) {flag=false;color = decideSWColor();setDefaultRules();}
		
		switch (msg.getType()) 
		{
	        case PACKET_IN:
	        	//return Command.STOP;
	        case PORT_STATUS:
	        	//color = decideSWColor();setDefaultRules();
	        	break;
	        default:
	            break;
        }
		//OFPacketIn pin = (OFPacketIn) msg;
		//OFMatch match = new OFMatch();
		//match.loadFromPacket(pin.getPacketData(), pin.getInPort());

		return Command.CONTINUE;
	}
	
	private Map<Long, swInfo> decideSWColor()//color
	{
		Map<Long, Set<Link>> SwitchLinks = new HashMap<Long, Set<Link>>();
		SwitchLinks = linkManager.getSwitchLinks();
		
		int size = SwitchLinks.size();//# of switches
		Map<Long, swInfo>color = new HashMap<Long, swInfo>(size);//initialCapacity

		System.out.println("=============="+size+"==============");
		Iterator<Entry<Long, Set<Link>>> ite = SwitchLinks.entrySet().iterator();
		while(ite.hasNext())
		{
			Set<Long> neighbor_SW = new HashSet<Long>();//If this set already contains the element, the call leaves the set unchanged and returns false.
			Entry<Long, Set<Link>> entry = ite.next(); 
			Long this_SW = entry.getKey();
			Set<Link> neighbor = entry.getValue();
			
			Iterator<Link> ite2 = neighbor.iterator();
			while(ite2.hasNext())
			{
				Link this_link = ite2.next();
				if(this_link.getSrc() != this_SW) neighbor_SW.add(this_link.getSrc());
				else if(this_link.getDst() != this_SW) neighbor_SW.add(this_link.getDst());
			} 
			Iterator<Long> ite3 = neighbor_SW.iterator();
			int[] color_flag =new int[8];//000~111 (0~7)
			for(int i=0;i<8;i++) color_flag[i] = 0;//initialization
			while(ite3.hasNext())
			{
				Long sw = ite3.next();	
				if(color.containsKey(sw)) color_flag[color.get(sw).getCOLOR()-1] = -1;//occupied
			}
			for(int i=0;i<8;i++) if(color_flag[i] != -1) 
			{
				color.put(this_SW, new swInfo(this_SW,i+1,neighbor_SW));
				break;
			}
		}
		//System.out.println(color.values());
		return color;
	}
	
	private void setDefaultRules()//default rules
	{
		Iterator<Entry<Long, swInfo>> ite = color.entrySet().iterator();
		while(ite.hasNext())
		{
			List<OFMessage> messages = new ArrayList<OFMessage>();//contain all default rules of a sw 
			Entry<Long, swInfo> entry = ite.next(); 
			Long DPID = entry.getKey();
			//Integer COLOR = entry.getValue().getCOLOR();
			
			Iterator<Long> neighbor = entry.getValue().getNeighbors().iterator();
			while(neighbor.hasNext())
			{
				Long neighbor_sw = neighbor.next();
				Integer neighbor_color = color.get(neighbor_sw).getCOLOR();
				//the default tag of (000) is reserved for the production traffic and is not used during the tag assignment process.
			
				OFMatch match = new OFMatch();
				match.setDataLayerVirtualLanPriorityCodePoint(neighbor_color.byteValue());
				match.setWildcards(~(OFMatch.OFPFW_DL_VLAN_PCP ));
				
				OFFlowMod mod = (OFFlowMod) floodlightProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
				mod.setMatch(match);
				mod.setCommand(OFFlowMod.OFPFC_ADD);
				mod.setIdleTimeout((short)0);
				mod.setHardTimeout((short)0);
				mod.setPriority((short)(32767));
				mod.setBufferId(OFPacketOut.BUFFER_ID_NONE);
				mod.setFlags((short)(1 << 0));
				
				List<OFAction> actions = new ArrayList<OFAction>();
				actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(),(short)0xFFFF));
				
				mod.setActions(actions);
				mod.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
				messages.add(mod);
			}
			//System.out.println(messages.size());
			writeOFMessagesToSwitch(DPID, messages);
		}
	}
	
	public void writeOFMessagesToSwitch(long dpid, List<OFMessage> messages) 
	{
    	IOFSwitch ofswitch = (IOFSwitch) floodlightProvider.getSwitch(dpid);

        if (ofswitch != null) {  // is the switch connected
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending {} new entries to {}", messages.size(), dpid);
                }
                ofswitch.write(messages, null);
                ofswitch.flush();
            } catch (IOException e) {
                logger.error("Tried to write to switch {} but got {}", dpid, e.getMessage());
            }
        }
    }
	
	private void pushPacket(IOFSwitch sw, OFMatch match, OFPacketIn pi, short outport)
	{
	    if (pi == null) {
	        return;
	    }

	    // The assumption here is (sw) is the switch that generated the
	    // packet-in. If the input port is the same as output port, then
	    // the packet-out should be ignored.
	    if (pi.getInPort() == outport) {
	        if (logger.isDebugEnabled()) {
	            logger.debug("Attempting to do packet-out to the same " +
	                      "interface as packet-in. Dropping packet. " +
	                      " SrcSwitch={}, match = {}, pi={}",
	                      new Object[]{sw, match, pi});
	            return;
	        }
	    }

	    if (logger.isTraceEnabled()) {
	        logger.trace("PacketOut srcSwitch={} match={} pi={}",
	                  new Object[] {sw, match, pi});
	    }

	    OFPacketOut po =
	            (OFPacketOut) floodlightProvider.getOFMessageFactory()
	                                            .getMessage(OFType.PACKET_OUT);

	    // set actions
	    List<OFAction> actions = new ArrayList<OFAction>();
	    actions.add(new OFActionOutput(outport, (short) 0xffff));
		//actions.add(new OFActionDataLayerDestination(Ethernet.toMACAddress(mac_10_0_0_1)));
		//actions.add(new OFActionNetworkLayerDestination(IPv4.toIPv4Address("10.0.0.1")));
		//actions.add(new OFActionTransportLayerDestination((short)5134));
		int length = OFActionOutput.MINIMUM_LENGTH            		
		       // + OFActionNetworkLayerDestination.MINIMUM_LENGTH 
	           // + OFActionDataLayerDestination.MINIMUM_LENGTH
	           // + OFActionTransportLayerDestination.MINIMUM_LENGTH
				;
	    po.setActions(actions)
	      .setActionsLength((short)length);
	    short poLength =
	            (short) (po.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);

	    // If the switch doens't support buffering set the buffer id to be none
	    // otherwise it'll be the the buffer id of the PacketIn
	    if (sw.getBuffers() == 0) {
	        // We set the PI buffer id here so we don't have to check again below
	        pi.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	        po.setBufferId(OFPacketOut.BUFFER_ID_NONE);
	    } else {
	        po.setBufferId(pi.getBufferId());
	    }

	    po.setInPort(pi.getInPort());

	    // If the buffer id is none or the switch doesn's support buffering
	    // we send the data with the packet out
	    if (pi.getBufferId() == OFPacketOut.BUFFER_ID_NONE) {
	        byte[] packetData = pi.getPacketData();
	        poLength += packetData.length;
	        po.setPacketData(packetData);
	    }

	    po.setLength(poLength);

	    try {
	        counterStore.updatePktOutFMCounterStoreLocal(sw, po);
	        sw.write(po, null);
	    } catch (IOException e) {
	        logger.error("Failure writing packet out", e);
	    }
	}

	@Override
	public void switchAdded(long switchId) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

	@Override
	public void switchRemoved(long switchId) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

	@Override
	public void switchActivated(long switchId) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

	@Override
	public void switchPortChanged(long switchId, ImmutablePort port,
			PortChangeType type) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

	@Override
	public void switchChanged(long switchId) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

	@Override
	public void topologyChanged(List<LDUpdate> linkUpdates) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

	@Override
	public void entityClassChanged(Set<String> entityClassNames) {
		// TODO Auto-generated method stub
		flag=true;color = decideSWColor();setDefaultRules();
	}

}