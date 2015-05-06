package org.cnv.shr.dmn;

import org.cnv.shr.mdl.LocalDirectory;

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

	public void localChanged(LocalDirectory local)
	{
		if (Services.application != null)
		{
			Services.application.refreshLocal(local);
		}
	}
}
