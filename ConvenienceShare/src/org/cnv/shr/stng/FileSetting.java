package org.cnv.shr.stng;

import java.awt.Container;
import java.io.File;

public class FileSetting extends Setting<File> 
{
	protected FileSetting(String n, File dv, boolean r, boolean u, String d)
	{
		super(n, dv, r, u, d);
	}

	@Override
	File parse(String vString)
	{
		return new File(vString);
	}

	@Override
	Container createInput()
	{
		return null;
	}
}
