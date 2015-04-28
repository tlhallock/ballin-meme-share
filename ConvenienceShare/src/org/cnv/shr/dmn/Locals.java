package org.cnv.shr.dmn;

import java.util.ArrayList;

import org.cnv.shr.mdl.LocalDirectory;

public class Locals
{

	private ArrayList<LocalDirectory> locals;

	private Locals()
	{
	}

	public LocalDirectory[] listLocals()
	{
		return locals.toArray(DUMMY);
	}

	private static Locals instance = new Locals();
	private static LocalDirectory[] DUMMY = new LocalDirectory[0];

	public static Locals getInstance()
	{
		return instance;
	}
}
