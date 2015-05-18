package org.cnv.shr.sync;

import org.cnv.shr.mdl.PathElement;

public class Pair
{
	private FileSource fsCopy;
	private PathElement dbCopy;
	
	public Pair(final FileSource f, final PathElement pathElement)
	{
		fsCopy = f;
		dbCopy = pathElement;
	}
	
	FileSource getFsCopy()
	{
		return fsCopy;
	}
	
	public PathElement getPathElement()
	{
		return dbCopy;
	}

	public FileSource getSource()
	{
		return fsCopy;
	}
}
