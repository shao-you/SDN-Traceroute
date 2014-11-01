package net.floodlightcontroller.traceroute;

import java.util.HashSet;
import java.util.Set;

public class swInfo {
	private Long DPID;
	private Integer COLOR;
	private Set<Long> NEIGHBORS;
	
	public swInfo(Long dpid, Integer color, Set<Long> neighbors)
	{
		DPID = dpid;
		COLOR = color;
		NEIGHBORS = neighbors;// = new HashSet<Long>();
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
	public boolean swExist(Long sw)
	{
		int sz = NEIGHBORS.size();
		//for(int i=0;i<sz;i++) 
		return false;
	}
}
