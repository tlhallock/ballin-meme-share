
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.dmn.not;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

import javax.swing.JFrame;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
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

public class Notifications implements WindowFocusListener
{
	private final LinkedList<WeakReference<NotificationListener>> weakListeners = new LinkedList<>();
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

	{ add(new LogListener()); }

	public void start()
	{
		stop = false;
		notificationThread.start();
	}
	
	public void stop()
	{
		stop = true;
		notificationThread.interrupt();
	}

	private synchronized List<NotificationListener> getListeners()
	{
		if (weakListeners.isEmpty())
		{
			List<NotificationListener> w = Collections.emptyList();
			return w;
		}
		
		LinkedList<NotificationListener> returnValue = new LinkedList<>();
		LinkedList<WeakReference<NotificationListener>> toDel = new LinkedList<>();
		for (WeakReference<NotificationListener> ref : weakListeners)
		{
			NotificationListener notificationListenerImpl = ref.get();
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
	
	public synchronized void add(final NotificationListener listener)
	{
		for (WeakReference<NotificationListener> ref : weakListeners)
		{
			NotificationListener l = ref.get();
			if (l == null)
			{
				continue;
			}
			if (l.equals(listener))
			{
				return;
			}
		}

		weakListeners.add(new WeakReference<NotificationListener>(listener));
	}
	
	public void permissionFailure(final PermissionFailureEvent event)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
				{
					listener.connectionClosed(c);
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
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
				for (final NotificationListener listener : getListeners())
				{
					listener.fileChanged(localFile);
				}
			}
		});
	}
	
	public void permissionsChanged(Machine remote)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : getListeners())
				{
					listener.permissionsChanged(remote);
				}
			}
		});
	}

	public void permissionsChanged(RemoteDirectory remote)
	{
		notifiers.add(new Runnable()
		{
			@Override
			public void run()
			{
				for (final NotificationListener listener : getListeners())
				{
					listener.permissionsChanged(remote);
				}
			}
		});
	}
	
	private static Point midPoint(JFrame frame)
	{
		return new Point(frame.getLocationOnScreen().x + frame.getWidth() / 2, frame.getLocationOnScreen().y + frame.getHeight() / 2);
	}
	private static Point upperLeft(JFrame frame, int centerX, int centerY)
	{
		return new Point(centerX - frame.getWidth() / 2, centerY - frame.getHeight() / 2);
	}
	
	public void setAppLocation(JFrame frame)
	{
		Point p = midPoint(frame);
		Services.settings.appLocX.set(p.x);
		Services.settings.appLocY.set(p.y);
	}
	
	public void registerWindow(final JFrame frame)
	{
        frame.setLocation(upperLeft(frame, Services.settings.appLocX.get(), Services.settings.appLocY.get()));
        frame.addWindowFocusListener(this);
				Services.colors.setColors(frame);
//		try
//		{
//			frame.setIconImage(Misc.getIcon());
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//		}
	}

	
	
	
	
	
	
	
	
	
	
	

	private Window inFocus;
//	@Override
//	public void windowOpened(WindowEvent e) {}
//	@Override
//	public void windowClosing(WindowEvent e) {}
//	@Override
//	public void windowClosed(WindowEvent e) {}
//	@Override
//	public void windowIconified(WindowEvent e) {}
//	@Override
//	public void windowDeiconified(WindowEvent e) {}
//	@Override
//	public void windowActivated(WindowEvent e) { inFocus = e.getWindow(); }
//	@Override
//	public void windowDeactivated(WindowEvent e) {}
	
	public Window getCurrentContext()
	{
		if (inFocus != null && !inFocus.isVisible()) inFocus = null;
		return inFocus;
	}

	@Override
	public void windowGainedFocus(WindowEvent e)
	{
		inFocus = e.getWindow();
	}
	@Override
	public void windowLostFocus(WindowEvent e) {}
}
