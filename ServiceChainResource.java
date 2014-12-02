package net.floodlightcontroller.traceroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class ServiceChainResource extends ServerResource {

    @Post
    public void monitorServiceChain(String fmJson) throws IOException {//return json
    	
    	pktInfo pkt = new pktInfo(fmJson);
		
		OFMatch match = new OFMatch();
		match.fromString(pkt.match);
		//System.out.println(pkt.match);
		//System.out.println(match.wildcards);
		if(match.getNetworkDestinationMaskLen() == 0) match.setWildcards(match.getWildcards() & OFMatch.OFPFW_ALL_SANITIZED);
		//System.out.println(match.wildcards);
		final int numOfCheckPoint = pkt.sw.size();
		for(int i=0;i<numOfCheckPoint;i++) Traceroute.matchTable.get(pkt.sw.get(i)).put(pkt.probePktId, match);		
		
		OFFlowMod mod = (OFFlowMod) Traceroute.floodlightProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
		mod.setMatch(match);
		mod.setCommand(OFFlowMod.OFPFC_ADD);
		mod.setIdleTimeout((short)0);
		mod.setHardTimeout((short)0);//0 means infinite, in seconds
		mod.setPriority((short)(32767));
		mod.setBufferId(OFPacketOut.BUFFER_ID_NONE);
		mod.setFlags((short)(1 << 0));

		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(new OFActionOutput(OFPort.OFPP_CONTROLLER.getValue(),(short)0xFFFF));
		
		mod.setActions(actions);
		mod.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
		List<OFMessage> messages = new ArrayList<OFMessage>();
		messages.add(mod);
		//System.out.println(messages.size());
		for(int i=0;i<numOfCheckPoint;i++) Traceroute.writeOFMessagesToSwitch(pkt.sw.get(i), messages);
    	System.out.println("==============================================");
    }
}
