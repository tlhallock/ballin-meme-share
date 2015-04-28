package org.cnv.shr.dmn;

public class Notifications
{
	public static void localsChanged()
	{
		Services.application.refreshLocals();
		// save locals...
	}
	
	public static void remotesChanged()
	{
		
	}
}
