package net.floodlightcontroller.traceroute;

import java.util.Set;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonSerialize(using=swInfo.class)
public class swInfo extends JsonSerializer<swInfo> {
	protected static Logger log = LoggerFactory.getLogger(swInfo.class);
	private Long DPID;
	private Integer COLOR;
	private Set<Long> NEIGHBORS;
	
	public swInfo() {}// Do NOT delete this, it's required for the serializer
	public swInfo(Long dpid, Integer color, Set<Long> neighbors)
	{
		DPID = dpid;
		COLOR = color;
		NEIGHBORS = neighbors;
	}
	public Long getDPID()
	{
		return DPID;
	}
	public Integer getCOLOR()
	{
		return COLOR;
	}
	public Set<Long> getNeighbors()
	{
		return NEIGHBORS;
	}
	
	@Override
    public void serialize(swInfo swi, JsonGenerator jgen, SerializerProvider arg2)
            throws IOException, JsonProcessingException {
    	    jgen.writeStartObject();
    	    jgen.writeStringField("dpid",Long.toString(swi.getDPID()));
    	    jgen.writeNumberField("color", swi.getCOLOR());
			jgen.writeEndObject();
    }

    @Override
    public Class<swInfo> handledType() {
        return swInfo.class;
    }
}
