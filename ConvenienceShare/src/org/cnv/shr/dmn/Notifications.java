package org.cnv.shr.dmn;


public class Notifications
{
	public static void localsChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshLocals();
		}
		Services.locals.write();
	}
	
	public static void remotesChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshRemotes();
		}
		Services.remotes.write();
	}
}
