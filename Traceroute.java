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
import org.openflow.protocol.OFPacketIn.OFPacketInReason;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;
import org.openflow.util.U16;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
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
 
	protected static IFloodlightProviderService floodlightProvider;
	protected static ICounterStoreService counterStore;
	protected static IDeviceService deviceManager;
	protected static IRoutingService routingEngine;
	protected static ILinkDiscoveryService linkManager;
	protected static ITopologyService topology;
	protected static IRestApiService restApi;
	
	protected static Logger logger;
	protected boolean flag = true;
	public static Map<Long, swInfo>color;
	public static Map<Byte, List<traceNode>> traceRoute;
	public static Map<Long, Map<Byte, OFMatch>> matchTable;
	
	Integer broadcast = IPv4.toIPv4Address("255.255.255.255");
	String mac = "ff:ff:ff:ff:ff:ff";
	
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
	        l.add(IRestApiService.class);
		return l;
		// TODO Auto-generated method stub
	}
 
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		counterStore = context.getServiceImpl(ICounterStoreService.class);
		routingEngine = context.getServiceImpl(IRoutingService.class);
		deviceManager = context.getServiceImpl(IDeviceService.class);
		linkManager = context.getServiceImpl(ILinkDiscoveryService.class);
		topology = context.getServiceImpl(ITopologyService.class);
		logger = LoggerFactory.getLogger(Traceroute.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		// TODO Auto-generated method stub
	}
 
	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);//註冊成為其他Service的Listener
		floodlightProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        floodlightProvider.addOFSwitchListener(this);
        topology.addListener(this);
        restApi.addRestletRoutable(new DebugUIWebRoutable());
        traceRoute = new HashMap<Byte, List<traceNode>>();
        matchTable = new HashMap<Long, Map<Byte, OFMatch>>() ;
		// TODO Auto-generated method stub
	}
 
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		OFPacketIn pin = (OFPacketIn) msg;
		OFMatch match = new OFMatch();
		match.loadFromPacket(pin.getPacketData(), pin.getInPort());
		
		if(msg.getType() == OFType.PACKET_IN && pin.getReason() == OFPacketInReason.ACTION)
		{
			if(matchTable.get(sw.getId()).size() > 0)//may be a monitoring packet, ttl is used to identify monitoring pkt
			{//only one pkt enters HERE//----------
				 Ethernet ethFrame = (Ethernet) new Ethernet().deserialize(pin.getPacketData(), 0, pin.getTotalLength());//BasePacket, IPacket
	    	     IPv4 ipv4Packet = (IPv4) ethFrame.getPayload();//BasePacket, IPacket
	    		 byte monitorid = ipv4Packet.getTtl();
				
				Map<Byte, OFMatch> table= matchTable.get(sw.getId());
				Iterator<Entry<Byte, OFMatch>> ite = table.entrySet().iterator();
				while(ite.hasNext())
				{
					Entry<Byte, OFMatch> entry = ite.next(); 
					if(entry.getValue().match(match) && entry.getKey() != monitorid)//first time
					{
				        //set ttl value
				        //System.out.println("Original ttl: "+ipv4Packet.getTtl());
		    			ipv4Packet.setTtl(entry.getKey()).setChecksum((short)0);
				        //System.out.println("Marked ttl: "+ipv4Packet.getTtl());
		    			pin.setPacketData(ethFrame.serialize());//write back to msg
						
		    			//pkt out with marked ttl
						List<OFAction> actions = new ArrayList<OFAction>();//actions in list will be executed in order
		    		    actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(), (short) 0xffff));
		    			pushPacket(sw, match, pin, OFPort.OFPP_TABLE.getValue(), actions, OFActionOutput.MINIMUM_LENGTH);
		    		    return Command.STOP;
					}
					else if(entry.getValue().match(match) && entry.getKey() == monitorid)//second time
					{
						//System.out.println("wildcard value: "+entry.getValue().wildcards);
						System.out.println("A monitoring packet is matched and forwarded to controller!");
						
						//remove flow entry in sw
				        OFMessage fm = ((OFFlowMod) floodlightProvider.getOFMessageFactory()
				            .getMessage(OFType.FLOW_MOD))
				            .setPriority((short)(32767))//should be the same to the priority of 'entry.getValue()'
				            .setMatch(entry.getValue())
				            .setCommand(OFFlowMod.OFPFC_DELETE_STRICT)//OFPFC_DELETE, OFPFC_DELETE_STRICT
				            .setOutPort(OFPort.OFPP_NONE)
				            .setLength(U16.t(OFFlowMod.MINIMUM_LENGTH));
				        
				        List<OFMessage> messages = new ArrayList<OFMessage>();
				        messages.add(fm);
				        writeOFMessagesToSwitch(sw.getId(), messages);
				        ite.remove();//------------
				        
				        //record checkpoint
				        System.out.println("check point: "+sw.getId()+", ttl: "+monitorid);
				        
				        //packet out without modification
		    			List<OFAction> actions = new ArrayList<OFAction>();//actions in list will be executed in order
		    		    actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(), (short) 0xffff));
		    			pushPacket(sw, match, pin, OFPort.OFPP_TABLE.getValue(), actions, OFActionOutput.MINIMUM_LENGTH);
		    		    return Command.STOP;
					}
				}
			}//end of pkt monitoring
		
			/*if(match.getDataLayerType()==Ethernet.TYPE_IPv4 && match.getDataLayerType()!=Ethernet.TYPE_ARP 
					&& match.getNetworkDestination()!=broadcast && match.getDataLayerDestination()!=Ethernet.toMACAddress(mac)
					&& (match.getNetworkProtocol()==IPv4.PROTOCOL_TCP || match.getNetworkProtocol()==IPv4.PROTOCOL_UDP || match.getNetworkProtocol()==IPv4.PROTOCOL_ICMP))
			*/
			if(match.getDataLayerVirtualLanPriorityCodePoint() != (byte)0)//filter the customized traffic
			{
			    		byte current_PKT_color = match.getDataLayerVirtualLanPriorityCodePoint();
			    		byte this_SW_color = -1;//no color
			    		if(color.containsKey(sw.getId())) this_SW_color = color.get(sw.getId()).getCOLOR().byteValue();
			    		else return Command.CONTINUE;//topology is not yet constructed completely
			    		
			    		if(current_PKT_color == this_SW_color) 
			    		{
			    			System.out.println("SW: "+sw.getId()+", Color is the same: "+this_SW_color);
			    			return Command.CONTINUE;
			    		}
			    		else//packet out the packet without FLOW_MOD
			    		{
			    			//record the route here
			    	        Ethernet ethFrame = (Ethernet) new Ethernet().deserialize(pin.getPacketData(), 0, pin.getTotalLength());//BasePacket, IPacket
			    	        //System.out.println(pin.getLength()+"=="+pin.getTotalLength()+"=="+msg.getLength());
			    	        IPv4 ipv4Packet = (IPv4) ethFrame.getPayload();//BasePacket, IPacket
			    			byte probeid = ipv4Packet.getTtl();
			    			//System.out.println(probeid);
			    			
			    	    	traceNode Node = new traceNode(sw.getId(),match.getInputPort(),(short)0);
			    	    	traceRoute.get(probeid).add(Node);
			    	    	
			    			System.out.println("SW: "+sw.getId()+", Pkt Color: "+current_PKT_color+", SW Color: "+this_SW_color);
			    			
			    			List<OFAction> actions = new ArrayList<OFAction>();//actions in list will be executed in order
			    			actions.add(new OFActionVirtualLanPriorityCodePoint(this_SW_color));
			    		    actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(), (short) 0xffff));
			    			pushPacket(sw, match, pin, OFPort.OFPP_TABLE.getValue(), actions, OFActionVirtualLanPriorityCodePoint.MINIMUM_LENGTH+OFActionOutput.MINIMUM_LENGTH);
			    			return Command.STOP;
			    		}
			}//end of probe tracing
			
		}//end of OFPacketInReason.ACTION
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
			if(!matchTable.containsKey(this_SW))//create table for each sw to store monitoring matches
			{
				Map<Byte,OFMatch> table = new HashMap<Byte, OFMatch>();
				matchTable.put(this_SW, table);
			}
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
				//the default tag of (000) is reserved for the production traffic and is not used during the tag assignment process
			
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
				actions.add(new OFActionOutput(OFPort.OFPP_CONTROLLER.getValue(),(short)0xFFFF));
				
				mod.setActions(actions);
				mod.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
				messages.add(mod);
			}
			//System.out.println(messages.size());
			writeOFMessagesToSwitch(DPID, messages);
		}
	}
	
	public static void writeOFMessagesToSwitch(long dpid, List<OFMessage> messages)
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
	
	private void pushPacket(IOFSwitch sw, OFMatch match, OFPacketIn pi, short outport, List<OFAction> actions, int actionLen)
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

	    po.setActions(actions)
	      .setActionsLength((short)actionLen);
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
	        sw.flush();
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