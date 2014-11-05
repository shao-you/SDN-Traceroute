package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;

import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.action.OFActionVirtualLanPriorityCodePoint;
import org.restlet.data.Form;  
import org.restlet.representation.Representation;  
import org.restlet.representation.StringRepresentation;  
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class TracerouteResource extends ServerResource {
	protected IPacket probePacket;
    @Post
    public List<traceNode> triggerTraceroute(Representation entity) {//return json
    	//parse pkt info

    	/* sw
    	 * dataLayerDestination, dataLayerSource, dataLayerType, dataLayerVLAN, dataLayerPCP, 
    	 * networkDestination, networkSource, networkProtocol, networkTypeOfService, 
    	 * transportDestination, transportSource//OpenFlow 1.0 cannot differentiate TCP from UDP
    	 * timestamp, service chain, loop, statistics
    	 */
    	
    	//generate corresponding packet and inject into sw
    	probePacket = new Ethernet()
        .setDestinationMACAddress("ff:00:00:08:ff:f9")
        .setSourceMACAddress("ff:ff:00:00:00:ff")
        .setEtherType(Ethernet.TYPE_IPv4)
        .setVlanID((short)999)
        .setPriorityCode((byte)7)
        .setPayload(
            new IPv4()
            .setProtocol((byte)17)//6 tcp, 17 udp
            .setDiffServ((byte)180)//ToS
            .setSourceAddress("199.168.10.10")
            .setDestinationAddress("199.168.10.20")
            .setPayload(new UDP()
                        .setSourcePort((short) 5000)
                        .setDestinationPort((short) 5001)
                        .setPayload(new Data(new byte[] {0x01}))));
    	
    	byte[] packetData = probePacket.serialize();
    	
    	Form form = new Form(entity);  
    	System.out.println(form.getWebRepresentation());
    	//trigger traceroute

    	IOFSwitch ofswitch = (IOFSwitch) Traceroute.floodlightProvider.getSwitch((long)3);
    	OFPacketOut packetOut = new OFPacketOut();
    	/*OFPacketOut packetOut =
	            (OFPacketOut) Traceroute.floodlightProvider.getOFMessageFactory()
	                                            .getMessage(OFType.PACKET_OUT);*/
    	
        packetOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        //packetOut.setInPort((short)1);
        packetOut.setPacketData(packetData);

	    // set actions
	    List<OFAction> actions = new ArrayList<OFAction>();//actions in list will be executed in order
	    actions.add(new OFActionOutput(OFPort.OFPP_TABLE.getValue(), (short) 0xffff));

		int length = OFActionOutput.MINIMUM_LENGTH;
		packetOut.setActions(actions)
	      .setActionsLength((short)length);
	    short poLength =
	            (short) (packetOut.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);
	     poLength += packetData.length;
	     packetOut.setLength(poLength);
	     packetOut.validate();
    	try {
			ofswitch.write(packetOut, null);
    		ofswitch.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//return traceroute results
        return Traceroute.traceRoute;
    }
    
}
