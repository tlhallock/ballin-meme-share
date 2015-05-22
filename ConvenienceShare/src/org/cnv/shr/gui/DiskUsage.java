package org.cnv.shr.gui;

import org.cnv.shr.util.Misc;

class DiskUsage implements Comparable<DiskUsage>
{
	private final long size;
	
	DiskUsage(long fSize)
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

class NumberOfFiles implements Comparable<NumberOfFiles>
{
	private final long numFiles;
	
	NumberOfFiles(long fSize)
	{
		this.numFiles = fSize;
	}
	
	public long get() { return numFiles; }
	
        @Override
	public String toString()
	{
		return Misc.formatNumberOfFiles(numFiles);
	}

	@Override
	public int compareTo(NumberOfFiles arg0)
	{
		return Long.compare(numFiles, arg0.numFiles);
	}
}