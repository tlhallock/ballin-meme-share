package org.cnv.shr.lcl;

import java.io.File;

import org.cnv.shr.mdl.PathElement;

public class Pair
{
	File fsCopy;
	PathElement dbCopy;
	
	public Pair(File f, PathElement pathElement)
	{
		fsCopy = f;
		dbCopy = pathElement;
	}
}
