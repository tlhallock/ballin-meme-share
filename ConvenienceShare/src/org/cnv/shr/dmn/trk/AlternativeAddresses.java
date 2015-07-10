package org.cnv.shr.dmn.trk;

import java.util.HashMap;
import java.util.HashSet;

import org.cnv.shr.trck.MachineEntry;

public class AlternativeAddresses
{
	public HashMap<String, HashSet<Integer>> alternativeIps = new HashMap<>();
	
	public String describe(String ip)
	{
		HashSet<Integer> set = alternativeIps.get(ip);
		if (set == null || set.isEmpty())
		{
			return "null";
		}

		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		
		// TODO: Looks bad if there were two different ranges, ie [1-2] and [1000-1001] becomes [1-10001]

		for (Integer i : set)
		{
			if (min > i)
			{
				min = i;
			}
			if (max < i)
			{
				min = i;
			}
		}
		
		return ip + ":[" + min + "-" + max + "]";
	}
	
	public boolean isEmpty()
	{
		for (HashSet<Integer> entry :  alternativeIps.values())
		{
			if (!entry.isEmpty())
			{
				return true;
			}
		}
		return false;
	}
	
	public void add(MachineEntry entry)
	{
		if (entry == null)
		{
			return;
		}
		HashSet<Integer> set = alternativeIps.get(entry.getIp());
		if (set == null)
		{
			set = new HashSet<>();
			alternativeIps.put(entry.getIp(), set);
		}
		for (int port = entry.getPortBegin(); port < entry.getPortEnd(); port++)
		{
			set.add(port);
		}
	}
	
	public void remove(String ip, int begin, int end)
	{
		HashSet<Integer> set = alternativeIps.get(ip);
		if (set == null)
		{
			return;
		}
		for (int port = begin; port < end; port++)
		{
			set.remove(port);
		}
		if (set.isEmpty())
		{
			alternativeIps.remove(ip);
		}
	}
	
	public Iterable<String> getIps()
	{
		return alternativeIps.keySet();
	}
	
	public Iterable<Integer> getPorts(String ip)
	{
		return alternativeIps.get(ip);
	}
}