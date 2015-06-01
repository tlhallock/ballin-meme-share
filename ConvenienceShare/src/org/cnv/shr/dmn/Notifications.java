package org.cnv.shr.dmn;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

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
import org.cnv.shr.util.LogWrapper;

public class Notifications
{
	private final LinkedList<WeakReference<NotificationListenerImpl>> weakListeners = new LinkedList<>();
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
					LogWrapper.getLogger().log(Level.INFO, "Unable to notify.", ex);
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

	private synchronized List<NotificationListenerImpl> getListeners()
	{
		if (weakListeners.isEmpty())
		{
			List<NotificationListenerImpl> w = Collections.emptyList();
			return w;
		}
		
		LinkedList<NotificationListenerImpl> returnValue = new LinkedList<>();
		LinkedList<WeakReference<NotificationListenerImpl>> toDel = new LinkedList<>();
		for (WeakReference<NotificationListenerImpl> ref : weakListeners)
		{
			NotificationListenerImpl notificationListenerImpl = ref.get();
			if (notificationListenerImpl == null)
			{
				toDel.add(ref);
			}
			else
			{
				returnValue.add(notificationListenerImpl);
			}
		}
		
		weakListeners.removeAll(toDel);
		return returnValue;
	}
	
	public synchronized void add(final NotificationListenerImpl listener)
	{
		for (WeakReference<NotificationListenerImpl> ref : weakListeners)
		{
			NotificationListenerImpl l = ref.get();
			if (l == null)
			{
				continue;
			}
			if (l.equals(listener))
			{
				return;
			}
		}

		weakListeners.add(new WeakReference<Notifications.NotificationListenerImpl>(listener));
	}
	
	public void permissionFailure(final PermissionFailureEvent event)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListenerImpl listener : getListeners())
				{
					listener.permissionFailure(event);
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
					LogWrapper.getLogger().log(Level.INFO, "Unable to close socket.", e);
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
				{
					listener.messageReceived(message);
				}
			}
		});
	}

	public void messagesChanged()
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListenerImpl listener : getListeners())
				{
					listener.messagesChanged();
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
				for (final NotificationListenerImpl listener : getListeners())
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
//			LogWrapper.getLogger().log(Level.INFO, , e);
//		}
	}

	public interface NotificationListenerImpl
	{
		void localsChanged()                                        ;
		void permissionFailure(final PermissionFailureEvent event)  ;
		void messageReceived(final UserMessage message)             ;
		void messagesChanged()                                      ;
		void localDirectoryChanged(final LocalDirectory local)      ;
		void remoteChanged(final Machine machine)                   ;
		void remotesChanged()                                       ;
		void remoteDirectoryChanged(final RemoteDirectory remote)   ;
		void downloadAdded(final DownloadInstance d)                ;
		void downloadRemoved(final DownloadInstance d)              ;
		void downloadDone(final DownloadInstance d)                 ;
		void serveAdded(final ServeInstance s)                      ;
		void serveRemoved(final ServeInstance s)                    ;
		void connectionOpened(final Communication c)                ;
		void connectionClosed(final Communication c)                ;
		void dbException(final Exception ex)                        ;
		void fileAdded(final SharedFile file)                       ;
		void fileChanged(final SharedFile file)                     ;
		void fileDeleted(final SharedFile file)                     ;
	
	}
	public static abstract class NotificationListener implements NotificationListenerImpl
	{
		public void localsChanged()                                        {}
		public void permissionFailure(final PermissionFailureEvent event)  {}
		public void messageReceived(final UserMessage message)             {}
		public void messagesChanged()                                      {}
		public void localDirectoryChanged(final LocalDirectory local)      {}
		public void remoteChanged(final Machine machine)                   {}
		public void remotesChanged()                                       {}
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
	}
}
