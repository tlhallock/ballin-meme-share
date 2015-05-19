package org.cnv.shr.gui;

import org.cnv.shr.util.Misc;

class DiskUsage implements Comparable<DiskUsage>
{
	private final long size;
	
	DiskUsage(long fSize)
	{
		this.size = fSize;
	}
	
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