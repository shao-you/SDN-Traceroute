package net.floodlightcontroller.traceroute;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonSerialize(using=traceNode.class)
public class traceNode extends JsonSerializer<traceNode> {
	protected static Logger log = LoggerFactory.getLogger(traceNode.class);
	private Long DPID;
	private Short InPort;
	private Short OutPort;
	
	public traceNode() {}// Do NOT delete this, it's required for the serializer
	public traceNode(Long dpid, Short inport, Short outport)
	{
		DPID = dpid;
		InPort = inport;
		OutPort = outport;
	}
	public Long getDPID()
	{
		return DPID;
	}
	public Short getInPort()
	{
		return InPort;
	}
	public Short getOutPort()
	{
		return OutPort;
	}
	
	@Override
    public void serialize(traceNode swi, JsonGenerator jgen, SerializerProvider arg2)
            throws IOException, JsonProcessingException {
    	    jgen.writeStartObject();
    	    jgen.writeStringField("dpid",Long.toString(swi.getDPID()));
    	    jgen.writeNumberField("inport", swi.getInPort());
    	    jgen.writeNumberField("outport", swi.getOutPort());
			jgen.writeEndObject();
    }

    @Override
    public Class<traceNode> handledType() {
        return traceNode.class;
    }
}
