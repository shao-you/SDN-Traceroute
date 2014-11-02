package net.floodlightcontroller.traceroute;

import java.util.Set;

public class swInfo {
	private Long DPID;
	private Integer COLOR;
	private Set<Long> NEIGHBORS;
	
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
}
