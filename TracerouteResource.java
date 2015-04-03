package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;

import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class TracerouteResource extends ServerResource {

    @Post
    public List<traceNode> triggerTraceroute(String fmJson) throws IOException {//return json
    	pktInfo pkt = new pktInfo(fmJson);
    	if(!pkt.onlyPktOut) pkt.dataLayerPCP = Traceroute.color.get(pkt.sw.get(0)).getCOLOR().byteValue();//color of sw
    	
    	byte[] bytes = new byte[pkt.dataSize];
    	Arrays.fill( bytes, (byte) 0x01 );
    	/***********generate corresponding packet and inject into sw***********/
    	IPacket probePacket = new Ethernet()
        .setDestinationMACAddress(pkt.dataLayerDestination)
        .setSourceMACAddress(pkt.dataLayerSource)
        .setEtherType(pkt.dataLayerType);
    	
    	if(pkt.dataLayerVLAN != -1 || pkt.dataLayerPCP != -1)
    	{
    		if(pkt.dataLayerVLAN == -1) pkt.dataLayerVLAN = 0;
    		if(pkt.dataLayerPCP == -1) pkt.dataLayerPCP = 0;
    		
    		((Ethernet)probePacket)
    			.setVlanID(pkt.dataLayerVLAN)
    			.setPriorityCode(pkt.dataLayerPCP);
    	}
    	
    	//System.out.println((byte)pkt.probePktId+"===");
    	IPv4 ipv4 = new IPv4()
            .setTtl(pkt.probePktId)//indicate a specific probe packet, not matched in OpenFlow, 8 bits
            .setDiffServ(pkt.networkTypeOfService)//ToS, 6 bits (0~63)
            .setSourceAddress(pkt.networkSource)
            .setDestinationAddress(pkt.networkDestination);
    	
    	if(pkt.networkProtocol == (byte)6)//tcp, at least 60 bytes
    	{
    		ipv4.setProtocol((byte)6).setChecksum((short)0);
    		ipv4.setPayload(new TCP()
	        .setSourcePort(pkt.transportSource)
	        .setDestinationPort(pkt.transportDestination)
	        .setChecksum((short)0)
	        .setPayload(new Data(bytes)));
    	}
    	else if(pkt.networkProtocol == (byte)17)//udp, at least 48 bytes
    	{
    		ipv4.setProtocol((byte)17).setChecksum((short)0);
    		ipv4.setPayload(new UDP()
            .setSourcePort(pkt.transportSource)
            .setDestinationPort(pkt.transportDestination)
            .setChecksum((short)0)
            .setPayload(new Data(bytes)));
    	}
    	else if(pkt.networkProtocol == (byte)1)//icmp, at least 44 bytes
    	{
    		ipv4.setProtocol((byte)1).setChecksum((short)0);
    		ipv4.setPayload(new ICMP()
			//.setIcmpType(ICMP.TIME_EXCEEDED)
			/*
			 * public static final byte ECHO_REPLY = 0x0;
			 * public static final byte ECHO_REQUEST = 0x8;
			 * public static final byte TIME_EXCEEDED = 0xB;
			 */
    		.setIcmpCode((byte)0)
            .setChecksum((short)0)
            .setPayload(new Data(bytes)));
    	}    	
        ((Ethernet)probePacket).setPayload(ipv4);

    	byte[] packetData = probePacket.serialize();

    	if(!pkt.onlyPktOut)
    	{
	    	if(Traceroute.traceRoute.containsKey(pkt.probePktId)) Traceroute.traceRoute.get(pkt.probePktId).clear();
	    	else Traceroute.traceRoute.put(pkt.probePktId, new ArrayList<traceNode>()); 
	    	
	    	traceNode firstNode = new traceNode(pkt.sw.get(0),pkt.inPort.get(0),(short)0);
	    	Traceroute.traceRoute.get(pkt.probePktId).add(firstNode);
    	}
    			
    	/***********trigger traceroute***********/
    	IOFSwitch ofswitch = (IOFSwitch) Traceroute.floodlightProvider.getSwitch(pkt.sw.get(0));
    	/*OFPacketOut packetOut = new OFPacketOut();*/
    	OFPacketOut packetOut =
	            (OFPacketOut) Traceroute.floodlightProvider.getOFMessageFactory()
	                                            .getMessage(OFType.PACKET_OUT);
    	
        packetOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        packetOut.setInPort(pkt.inPort.get(0));
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
	     //System.out.println("-------"+packetData.length);
	     packetOut.setLength(poLength);
	     packetOut.validate();
    	try {
			ofswitch.write(packetOut, null);
    		ofswitch.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	/***********wait for execution & return traceroute results***********/
    	if(!pkt.onlyPktOut)
    	{
	    	try {
				Thread.sleep(1000);//millisecond, time must be large enough for probe packet traveling
				checkLoop(Traceroute.traceRoute.get(pkt.probePktId));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	//System.out.println("====================== traceroute ======================");
	        return Traceroute.traceRoute.get(pkt.probePktId);
    	}
    	else 
    	{
    		//System.out.println("====================== only probe ======================");
    		return null;//only pkt-out a pkt 
    	}
    }
    
    private void checkLoop(List<traceNode> trace)
    {
    	for(int i=0;i<trace.size();i++)
    	{
    		for(int j=0;j<i;j++) 
    			if(trace.get(i).getDPID()==trace.get(j).getDPID())
    			{
    				trace.get(i).setRepeatNode(true);
    				break;
    			}
    	}
    }
    
}
