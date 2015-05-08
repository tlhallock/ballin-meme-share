package org.cnv.shr.dmn;

import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.RemoteDirectory;

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

	public void localChanged(LocalDirectory local)
	{
		if (Services.application != null)
		{
			Services.application.refreshLocal(local);
		}
	}
	
	public void remotesChanged()
	{
		if (Services.application != null)
		{
			Services.application.refreshRemotes();
		}
//		Services.remotes.write();
	}
	public void remotesChanged(RemoteDirectory remote)
	{
		if (Services.application != null)
		{
			Services.application.refreshRemote(remote);
		}
//		Services.remotes.write();
	}
}
