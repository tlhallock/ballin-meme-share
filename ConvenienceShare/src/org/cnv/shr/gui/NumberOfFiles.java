package org.cnv.shr.gui;

import org.cnv.shr.util.Misc;

public class NumberOfFiles implements Comparable<NumberOfFiles>
{
	private final long numFiles;
	
	public NumberOfFiles(long fSize)
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