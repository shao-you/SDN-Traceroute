package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class TracerouteResource extends ServerResource {

    @Post
    public List<traceNode> triggerTraceroute(String fmJson) throws IOException {//return json
    	
    	/***********variable declaration***********/
    	int probePktId = (int)(Math.random()*256);//ran.nextInt();//max 256 probe packets, 1~255 (ttl=0 is not used)----------------
    	long sw = 3;
    	short inPort = 1;
    	//timestamp, service chain, loop, statistics
    	String dataLayerDestination = "00:00:00:00:00:08";//"11:22:33:44:55:66";
    	String dataLayerSource = "00:00:00:00:00:01";//"66:55:44:33:22:11";
    	short dataLayerType = 0x0800;//only support Ethernet.TYPE_IPv4
        /*
         * public static final short TYPE_ARP = (short) 0x0806;
         * public static final short TYPE_RARP = (short) 0x8035;
         * public static final short TYPE_IPv4 = (short) 0x0800;
         * public static final short TYPE_LLDP = (short) 0x88cc;
         * public static final short TYPE_BSN = (short) 0x8942;
         */
    	short dataLayerVLAN = (short)0;
    	byte dataLayerPCP = 1;//cannot be specified by user
    	String networkDestination = "10.0.0.8";//"192.168.10.10";
    	String networkSource = "10.0.0.1";//"192.168.20.20";
    	byte networkProtocol = 1;//IPv4.PROTOCOL_UDP
        /*
         * public static final byte PROTOCOL_ICMP = 0x1;
         * public static final byte PROTOCOL_TCP = 0x6;
         * public static final byte PROTOCOL_UDP = 0x11;
         */
    	byte networkTypeOfService = 101;
    	short transportSource = 5000;//OpenFlow 1.0 cannot differentiate TCP from UDP
    	short transportDestination = 5001;

    	/***********parse pkt info***********/
    	MappingJsonFactory f = new MappingJsonFactory();
        JsonParser jp;
        
        try {
            jp = f.createJsonParser(fmJson);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
        
        jp.nextToken();
        
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw new IOException("Expected START_OBJECT");
        }
        
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            if (jp.getCurrentToken() != JsonToken.FIELD_NAME) {
                throw new IOException("Expected FIELD_NAME");
            }
            
            String n = jp.getCurrentName();
            jp.nextToken();
            if (jp.getText().equals("")) 
                continue;
            //System.out.println(n+"============"+jp.getText());
            
            if (n == "sw") sw = jp.getLongValue();//jp.getValueAsLong();
            else if(n.equals("inPort")) inPort = jp.getShortValue();
            else if(n.equals("dataLayerDestination")) dataLayerDestination = jp.getText();
            else if(n.equals("dataLayerSource")) dataLayerSource = jp.getText();
            else if(n.equals("dataLayerType")) dataLayerType = jp.getShortValue();
            else if(n.equals("dataLayerVLAN")) dataLayerVLAN = jp.getShortValue();
            //else if(n.equals("dataLayerPCP")) dataLayerPCP = jp.getByteValue();
            else if(n.equals("networkDestination")) networkDestination = jp.getText();
            else if(n.equals("networkSource")) networkSource = jp.getText();
            else if(n.equals("networkProtocol")) networkProtocol = jp.getByteValue();
            else if(n.equals("networkTypeOfService")) networkTypeOfService = jp.getByteValue();
            else if(n.equals("transportDestination")) transportDestination = jp.getShortValue();
            else if(n.equals("transportSource")) transportSource = jp.getShortValue();
        }
    	dataLayerPCP = Traceroute.color.get(sw).getCOLOR().byteValue();//color of sw
    	
    	/***********generate corresponding packet and inject into sw***********/
    	IPacket probePacket = new Ethernet()
        .setDestinationMACAddress(dataLayerDestination)
        .setSourceMACAddress(dataLayerSource)
        .setEtherType(dataLayerType)
        .setVlanID(dataLayerVLAN)
        .setPriorityCode(dataLayerPCP)
        .setPayload(
            new IPv4()
            .setTtl((byte)probePktId)//indicate a specific probe packet, not matched in OpenFlow
            .setProtocol(networkProtocol)//6 tcp, 17 udp, 1 icmp
            .setDiffServ(networkTypeOfService)//ToS
            .setSourceAddress(networkSource)
            .setDestinationAddress(networkDestination)
            .setChecksum((short)0)
            
            .setPayload(new ICMP()//---------
            			//.setIcmpType(ICMP.TIME_EXCEEDED)
            			/*
            			 * public static final byte ECHO_REPLY = 0x0;
    					 * public static final byte ECHO_REQUEST = 0x8;
    					 * public static final byte TIME_EXCEEDED = 0xB;
    					 */
                        .setChecksum((short)0)
                        .setPayload(new Data(new byte[] {0x01}))));
            
            /*.setPayload(new TCP()//---------
				        .setSourcePort(transportSource)
				        .setDestinationPort(transportDestination)
				        .setChecksum((short)0)
				        .setPayload(new Data(new byte[] {0x01}))));*/
    	
            /*.setPayload(new UDP()//---------
                        .setSourcePort(transportSource)
                        .setDestinationPort(transportDestination)
                        .setChecksum((short)0)
                        .setPayload(new Data(new byte[] {0x01}))));*/
    	
    	byte[] packetData = probePacket.serialize();
    	List<traceNode> trace = new ArrayList<traceNode>();
    	traceNode firstNode = new traceNode(sw,inPort,(short)0);
    	trace.add(firstNode);
    	Traceroute.traceRoute.put((byte)probePktId, trace);//--------------------------------------------------------
    	//System.out.println((byte)probePktId+"===");
    			
    	/***********trigger traceroute***********/
    	IOFSwitch ofswitch = (IOFSwitch) Traceroute.floodlightProvider.getSwitch(sw);
    	/*OFPacketOut packetOut = new OFPacketOut();*/
    	OFPacketOut packetOut =
	            (OFPacketOut) Traceroute.floodlightProvider.getOFMessageFactory()
	                                            .getMessage(OFType.PACKET_OUT);
    	
        packetOut.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        packetOut.setInPort(inPort);
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
    	try {
			Thread.sleep(1000);//millisecond, time must be large enough for probe packet traveling
			checkLoop(Traceroute.traceRoute.get((byte)probePktId));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.println("==============================================");
        return Traceroute.traceRoute.get((byte)probePktId);
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
