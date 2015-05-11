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
	}
	public void remotesChanged(RemoteDirectory remote)
	{
		if (Services.application != null)
		{
			Services.application.refreshRemote(remote);
		}
	}
	
	public void downloadsChanged()
	{
		
	}

	public void servesChanged()
	{
		
	}
	
	public void connectionOpened(Communication c)
	{
		
	}
	
	public void connectionClosed(Communication c)
	{
		
	}
	
	public void dbException(Exception ex)
	{
		
	}
}
