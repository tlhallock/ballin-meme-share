package org.cnv.shr.dmn;

import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.RemoteDirectory;

public class Notifications
{
	LinkedList<NotificationListener> listeners = new LinkedList<>();
	
	public void add(NotificationListener listener)
	{
		if (!listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}
	public void remove(NotificationListener listener)
	{
		listeners.remove(listener);
	}
	
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
	
	public void downloadAdded(DownloadInstance d)
	{
		for (NotificationListener listener : listeners)
		{
			listener.downloadAdded(d);
		}
	}
	public void downloadRemoved(DownloadInstance d)
	{
		for (NotificationListener listener : listeners)
		{
			listener.downloadRemoved(d);
		}
	}
	public void downloadDone(DownloadInstance d)
	{
		for (NotificationListener listener : listeners)
		{
			listener.downloadDone(d);
		}
	}

	public void serveAdded(ServeInstance serveInstance)
	{
		
	}

	public void serveRemoved(ServeInstance serveInstance)
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
		for (NotificationListener listener : listeners)
		{
			listener.dbException(ex);
		}
	}
	
	public static abstract class NotificationListener
	{
		public void localsChanged()                          {}
		public void localChanged(LocalDirectory local)       {}
		public void remotesChanged()                         {}
		public void remotesChanged(RemoteDirectory remote)   {}
		public void downloadAdded(DownloadInstance d)        {}
		public void downloadRemoved(DownloadInstance d)      {}
		public void downloadDone(DownloadInstance d)         {}
		public void serveAdded(ServeInstance s)              {}
		public void serveRemoved(ServeInstance s)            {}
		public void connectionOpened(Communication c)        {}
		public void connectionClosed(Communication c)        {}
		public void dbException(Exception ex)                {}
	}
}
