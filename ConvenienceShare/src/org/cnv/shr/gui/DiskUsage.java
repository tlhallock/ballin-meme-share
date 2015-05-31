package org.cnv.shr.gui;

import org.cnv.shr.util.Misc;

public class DiskUsage implements Comparable<DiskUsage>
{
	private final long size;
	
	public DiskUsage(long fSize)
	{
		this.size = fSize;
	}
	
	public long get() { return size; }
	
        @Override
	public String toString()
	{
		return Misc.formatDiskUsage(size);
	}

	@Override
	public int compareTo(DiskUsage arg0)
	{
		return Long.compare(size, arg0.size);
	}
}