package net.floodlightcontroller.traceroute;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class SwTagResource extends ServerResource {
    /*@Get("json")
    public List<swInfo> retrieve() {
    	List<swInfo> routePair = new ArrayList<swInfo>();
    	Map<Long, swInfo>color = Traceroute.color;
    	
		Iterator<Entry<Long, swInfo>> ite = color.entrySet().iterator();
		while(ite.hasNext())
		{
			Entry<Long, swInfo> entry = ite.next(); 
			//Long DPID = entry.getKey();
			routePair.add(entry.getValue());
		}
        return routePair;
    }*/
    @Get("json")
    public Map<Long, swInfo> retrieve() {
        return Traceroute.color;
    }
}
