package org.cnv.shr.dmn;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;

import javax.swing.JFrame;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.msg.key.PermissionFailure.PermissionFailureEvent;

public class Notifications
{
	private final LinkedList<NotificationListener> listeners = new LinkedList<>();
	private boolean stop;
	private final LinkedBlockingDeque<Runnable> notifiers = new LinkedBlockingDeque<>();
	private final Thread notificationThread = new Thread(new Runnable() {
		@Override
		public void run()
		{
			while (!stop)
			{
				try
				{
					notifiers.take().run();
				}
				catch (final Exception ex)
				{
					Services.logger.print(ex);
				}
			}
		}});

	void start()
	{
		stop = false;
		notificationThread.start();
	}
	void stop()
	{
		stop = true;
		notificationThread.interrupt();
	}
	
	public void add(final NotificationListener listener)
	{
		if (!listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}
	public void remove(final NotificationListener listener)
	{
		listeners.remove(listener);
	}
	
	public void permissionFailure(final PermissionFailureEvent event)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.permissionFailure(event);
				}
			}
		});
	}

	public void lineLogged(final String line)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.lineLogged(line);
				}
			}
		});
	}

	public void localsChanged()
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.localsChanged();
				}
			}
		});
	}

	public void localChanged(final LocalDirectory local)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.localDirectoryChanged(local);
				}
			}
		});
	}

	public void remoteChanged(final Machine machine)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.remoteChanged(machine);
				}
			}
		});
	}

	public void remotesChanged()
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.remotesChanged();
				}
			}
		});
	}

	public void remoteDirectoryChanged(final RemoteDirectory remote)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.remoteDirectoryChanged(remote);
				}
			}
		});
	}

	public void downloadAdded(final DownloadInstance d)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.downloadAdded(d);
				}
			}
		});
	}

	public void downloadRemoved(final DownloadInstance d)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.downloadRemoved(d);
				}
			}
		});
	}

	public void downloadDone(final DownloadInstance d)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.downloadDone(d);
				}
			}
		});
	}

	public void serveAdded(final ServeInstance serveInstance)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.serveAdded(serveInstance);
				}
			}
		});
	}

	public void serveRemoved(final ServeInstance serveInstance)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.serveRemoved(serveInstance);
				}
			}
		});
	}

	public void connectionOpened(final Communication c)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.connectionOpened(c);
				}
			}
		});
	}

	public void connectionClosed(final Communication c)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
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
				catch (final IOException e)
				{
					Services.logger.print(e);
				}
			}
		});
	}

	public void dbException(final Exception ex)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.dbException(ex);
				}
			}
		});
	}

	public void messageReceived(final UserMessage message)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.messageReceived(message);
				}
			}
		});
	}

	public void fileAdded(final SharedFile lFile)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.fileAdded(lFile);
				}
			}
		});
	}

	public void fileChanged(final SharedFile lFile)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.fileChanged(lFile);
				}
			}
		});
	}

	public void fileDeleted(final LocalFile localFile)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : listeners)
				{
					listener.fileChanged(localFile);
				}
			}
		});
	}
	
	public void registerWindow(final JFrame frame)
	{
        frame.setLocation(Services.settings.appLocX.get(), Services.settings.appLocY.get());
//		try
//		{
//			frame.setIconImage(Misc.getIcon());
//		}
//		catch (IOException e)
//		{
//			Services.logger.print(e);
//		}
	}
	
	public static abstract class NotificationListener
	{
		public void localsChanged()                                  {}
		public void permissionFailure(final PermissionFailureEvent event)  {}
		public void messageReceived(final UserMessage message)             {}
		public void localDirectoryChanged(final LocalDirectory local)      {}
		public void remoteChanged(final Machine machine)                   {}
		public void remotesChanged()                                 {}
		public void remoteDirectoryChanged(final RemoteDirectory remote)   {}
		public void downloadAdded(final DownloadInstance d)                {}
		public void downloadRemoved(final DownloadInstance d)              {}
		public void downloadDone(final DownloadInstance d)                 {}
		public void serveAdded(final ServeInstance s)                      {}
		public void serveRemoved(final ServeInstance s)                    {}
		public void connectionOpened(final Communication c)                {}
		public void connectionClosed(final Communication c)                {}
		public void dbException(final Exception ex)                        {}
		public void fileAdded(final SharedFile file)                       {}
		public void fileChanged(final SharedFile file)                     {}
		public void fileDeleted(final SharedFile file)                     {}
		public void lineLogged(final String line)                          {}
	}
}
