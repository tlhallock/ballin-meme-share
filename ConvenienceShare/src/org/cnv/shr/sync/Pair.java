package org.cnv.shr.sync;

import org.cnv.shr.mdl.PathElement;

public class Pair<T extends FileSource>
{
	private T fsCopy;
	private PathElement dbCopy;
	
	public Pair(T f, PathElement pathElement)
	{
		fsCopy = f;
		dbCopy = pathElement;
	}
	
	T getFsCopy()
	{
		return fsCopy;
	}
	
	PathElement getPathElement()
	{
		return dbCopy;
	}

	public T getSource()
	{
		return fsCopy;
	}
}
