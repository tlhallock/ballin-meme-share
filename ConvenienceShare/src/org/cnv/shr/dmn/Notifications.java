package org.cnv.shr.dmn;

public class Notifications
{   
	public void localsChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshLocals();
		}
//		Services.locals.write();
	}
	
	public void remotesChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshRemotes();
		}
//		Services.remotes.write();
	}
}
