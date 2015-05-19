package org.cnv.shr.dmn;

import java.io.IOException;
import java.util.LinkedList;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.UserMessage;

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
		for (NotificationListener listener : listeners)
		{
			listener.localsChanged();
		}
	}

	public void localChanged(LocalDirectory local)
	{
		for (NotificationListener listener : listeners)
		{
			listener.localChanged(local);
		}
	}
	
	public void remotesChanged()
	{
		for (NotificationListener listener : listeners)
		{
			listener.remotesChanged();
		}
	}
	public void remotesChanged(RemoteDirectory remote)
	{
		for (NotificationListener listener : listeners)
		{
			listener.remotesChanged(remote);
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
		for (NotificationListener listener : listeners)
		{
			listener.serveAdded(serveInstance);
		}
	}

	public void serveRemoved(ServeInstance serveInstance)
	{
		for (NotificationListener listener : listeners)
		{
			listener.serveRemoved(serveInstance);
		}
	}
	
	public void connectionOpened(Communication c)
	{
		for (NotificationListener listener : listeners)
		{
			listener.connectionOpened(c);
		}
	}
	
	public void connectionClosed(Communication c)
	{
		for (NotificationListener listener : listeners)
		{
			listener.connectionClosed(c);
		}
		try
		{
			if (!c.getSocket().isClosed())
			{
				c.getSocket().close();
			}
		}
		catch(IOException e)
		{
			Services.logger.print(e);
		}
	}
	
	public void dbException(Exception ex)
	{
		for (NotificationListener listener : listeners)
		{
			listener.dbException(ex);
		}
	}

	public void messageReceived(UserMessage message)
	{
		for (NotificationListener listener : listeners)
		{
			listener.messageReceived(message);
		}
	}
	
	public static abstract class NotificationListener
	{
		public void localsChanged()                          {}
		public void messageReceived(UserMessage message)     {}
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
