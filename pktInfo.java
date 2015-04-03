package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public class pktInfo {
	
		/***********variable declaration***********/
		String match = "";//store match field in string format
		byte probePktId =(byte) (new Random().nextInt(255)+1);//(byte)((Math.random()*255)+1);//max 256 probe packets, 1~255 (ttl=0 is not used)
		Boolean monitorMode = false;
		Boolean onlyPktOut = false;
		Integer dataSize = 1;
		
    	List<Long> sw = new ArrayList<Long>();
    	List<Short> inPort = new ArrayList<Short>();
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
    	short dataLayerVLAN = -1;//12 bits
    	byte dataLayerPCP = -1;//3 bits, cannot be specified by user on 'defaultMode'
    	String networkDestination = "10.0.0.8";//"192.168.10.10";
    	String networkSource = "10.0.0.1";//"192.168.20.20";
    	byte networkProtocol = 0x11;
        /*
         * public static final byte PROTOCOL_ICMP = 0x1;
         * public static final byte PROTOCOL_TCP = 0x6;
         * public static final byte PROTOCOL_UDP = 0x11;
         */
    	byte networkTypeOfService = 101;
    	short transportSource = 5000;//OpenFlow 1.0 cannot differentiate TCP from UDP
    	short transportDestination = 5001;
    	
	public pktInfo(String fmJson)  throws IOException
	{
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
            if(n.equals("monitorMode"))
            {
            	monitorMode = jp.getBooleanValue();
            }
            else if(n.equals("onlyPktOut"))
            {
            	onlyPktOut = jp.getBooleanValue();
            }
            else if(n.equals("dataSize")) 
            {
            	dataSize = jp.getIntValue();
            }
            else if (n.equals("sw")) 
            {
            	if (jp.getCurrentToken() == JsonToken.START_ARRAY) 
            	{
                    // For each of the records in the array
                    while (jp.nextToken() != JsonToken.END_ARRAY) 
                    {
                      // read the record into a tree model,
                      // this moves the parsing position to the end of it
                      //JsonNode Node = jp.readValueAsTree();
                      sw.add(jp.getLongValue());
                      //System.out.println("sw: " + jp.getLongValue());
                    }
            	}
            	else sw.add(jp.getLongValue());
            }
            else if(n.equals("inPort")) 
            {
            	if (jp.getCurrentToken() == JsonToken.START_ARRAY) 
            	{
                    // For each of the records in the array
                    while (jp.nextToken() != JsonToken.END_ARRAY) 
                    {
                      // read the record into a tree model,
                      // this moves the parsing position to the end of it
                      //JsonNode Node = jp.readValueAsTree();
                      inPort.add(jp.getShortValue());
                      //System.out.println("inport: " + jp.getShortValue());
                    }
            	}
            	else 
            	{
            		 inPort.add(jp.getShortValue()); 
            		 match = match + "in_port=" + jp.getText() + ",";
            	}
            }
            else if(n.equals("dataLayerDestination")) {dataLayerDestination = jp.getText(); match = match + "dl_dst=" + jp.getText() + ",";}
            else if(n.equals("dataLayerSource")) {dataLayerSource = jp.getText(); match = match + "dl_src=" + jp.getText() + ",";}
            else if(n.equals("dataLayerType")) {dataLayerType = jp.getShortValue(); match = match + "dl_type=" + jp.getText() + ",";}
            else if(n.equals("dataLayerVLAN")) {dataLayerVLAN = jp.getShortValue(); match = match + "dl_vlan=" + jp.getText() + ",";}
            else if(n.equals("dataLayerPCP")) {dataLayerPCP = jp.getByteValue(); match = match + "dl_vlan_pcp=" + jp.getText() + ",";}
            else if(n.equals("networkDestination")) {networkDestination = jp.getText(); match = match + "nw_dst=" + jp.getText() + ",";}
            else if(n.equals("networkSource")) {networkSource = jp.getText(); match = match + "nw_src=" + jp.getText() + ",";}
            else if(n.equals("networkProtocol")) {networkProtocol = jp.getByteValue(); match = match + "nw_proto=" + jp.getText() + ",";}
            else if(n.equals("networkTypeOfService")) {networkTypeOfService = jp.getByteValue(); match = match + "nw_tos=" + jp.getText() + ",";}
            else if(n.equals("transportDestination")) {transportDestination = jp.getShortValue(); match = match + "tp_dst=" + jp.getText() + ",";}
            else if(n.equals("transportSource")) {transportSource = jp.getShortValue(); match = match + "tp_src=" + jp.getText() + ",";}
        }
	}
	
}
