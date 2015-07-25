
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



package org.cnv.shr.dmn;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javax.swing.ImageIcon;

import org.cnv.shr.cnctn.ConnectionManager;
import org.cnv.shr.cnctn.HandshakeServer;
import org.cnv.shr.db.h2.DbConnectionCache;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.bak.DbBackupRestore;
import org.cnv.shr.dmn.dwn.DownloadManager;
import org.cnv.shr.dmn.dwn.ServeManager;
import org.cnv.shr.dmn.mn.Arguments;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.dmn.mn.Quiter;
import org.cnv.shr.dmn.not.Notifications;
import org.cnv.shr.dmn.trk.ClientTrackerClient;
import org.cnv.shr.dmn.trk.TrackerFrame;
import org.cnv.shr.dmn.trk.Trackers;
import org.cnv.shr.gui.SplashScreen;
import org.cnv.shr.gui.TaskMenu;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.gui.color.ColorSetter;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.Machine.LocalMachine;
import org.cnv.shr.msg.MessageReader;
import org.cnv.shr.stng.SettingListener;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.sync.RemoteSynchronizers;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.updt.UpdateInfo;
import org.cnv.shr.updt.UpdateInfoImpl;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.NonRejectingExecutor;
import org.cnv.shr.util.PortMapper;

public class Services
{
	/** To take items off the AWT event thread **/
	public static ExecutorService userThreads;
	/** To handle outgoing network traffic **/
	public static NonRejectingExecutor connectionThreads;
	/** To handle incoming network traffic **/
	private static ServerSocket serveSocket;
	public static HandshakeServer connectionServer;
	public static ServerSocket[] sockets;
	
	public static Settings settings;
	/** delete this **/
	public static Notifications notifications;
	public static ChecksumManager checksums;
	public static ConnectionManager networkManager;
	public static MessageReader msgReader;
	public static KeysService keyManager;
	public static LocalMachine localMachine;
	public static DbConnectionCache h2DbCache;
	public static ServeManager server;
	public static DownloadManager downloads;
	public static RemoteSynchronizers syncs;
	public static BlackList blackList;
	public static Quiter quiter;
	public static UpdateManager updateManager;
	public static UpdateInfo codeUpdateInfo;
	public static Trackers trackers;
	public static ColorSetter colors;
	
	private static final Object localSyncerSync = new Object();
	private static TimerTask localSynchronizer;
	public static CompressionList compressionManager;
	
	public static void initialize(Arguments args, SplashScreen screen) throws Exception
	{
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Uncaught exception in thread " + t.getName() + ":" + t.getId(), e);
			}
		});
		
		testStartUp(screen);
		createServices(args.settings, screen);
		if (args.restoreFile != null)
		{
			DbBackupRestore.restoreDatabase(args.restoreFile, true);
		}
		checkIfUpdateManagerIsRunning(args, screen);
		startServices(screen);

		if (screen != null)
			screen.setStatus("Starting gui");
		if (args.showGui)
		{
			LogWrapper.getLogger().warning("This should only happen after the db has been restored...");
			UserActions.showGui(screen);
		}
		else if (screen != null)
		{
			screen.dispose();
		}
		
		System.out.println(Misc.INITIALIZED_STRING);
		System.out.println("-------------------------------------------------------------------------");
		System.out.flush();
		
		runStartupServices();
	}


	private static void runStartupServices()
	{
		userThreads.execute(() ->
		{
			if (settings.runTrackerOnStart.get())
			{
				Trackers.launchTracker(false);
			}
		});
		userThreads.execute(() ->
		{
			if (settings.autoMapPorts.get())
			{
				PortMapper.addDesiredPorts(settings.getLocalIp(), System.out);
			}
		});
		userThreads.execute(() ->
		{
			for (ClientTrackerClient client : trackers.getClients())
			{
				TrackerEntry entry = client.getEntry();
				if (entry.shouldSync() && entry.supportsMetaData())
				{
					try
					{
						client.sync();
					}
					catch (Exception ex)
					{
						LogWrapper.getLogger().log(Level.INFO, "Unable to sync to " + entry.toDebugString(), ex);
					}
				}
			}
		});
	}
	
	
	private static void createServices(Settings stgs, SplashScreen screen) throws Exception
	{
		if (screen != null)
			screen.setStatus("Creating services");
		compressionManager = new CompressionList(); compressionManager.read(settings.compressionFile.getPath());
		colors = new ColorSetter();
		colors.read();
		notifications = new Notifications();
		keyManager = new KeysService();
		keyManager.readKeys(Services.settings.keysFile.getPath(), Services.settings.keySize.get());
		localMachine = new Machine.LocalMachine();
		DbMachines.cleanOldLocalMachine();
		if (!localMachine.tryToSave())
		{
			localMachine.setId();
		}
		DbKeys.addKey(localMachine, keyManager.getPublicKey());
		networkManager = new ConnectionManager();
		msgReader = new MessageReader();
		server = new ServeManager();
		downloads = new DownloadManager();
		syncs = new RemoteSynchronizers();
		blackList = new BlackList();
		updateManager = new UpdateManager(Settings.getVersion(Main.class));
		updateManager.read();
		
		trackers = new Trackers();
		trackers.load(settings.trackerFile.getPath());

		connectionServer = new HandshakeServer(
				serveSocket,
				settings.servePortBeginI.get() + 1,
				settings.servePortBeginI.get() + settings.numHandlers.get());
		
		userThreads        = Executors.newCachedThreadPool();
		connectionThreads  = new NonRejectingExecutor("cnctns", settings.maxDownloads.get());
		checksums = new ChecksumManager();
		
		userThreads.execute(() -> { settings.setIpAddress(); });
		
		startSystemTray(screen);
	}


	private static void checkIfUpdateManagerIsRunning(Arguments a, SplashScreen screen) throws Exception
	{
		if (screen != null)
		{
			screen.setStatus("Checking if update manager is running");
		}
		if (a.updateManagerDirectory == null)
		{
			return;
		}
		LogWrapper.getLogger().info("We have a an update manager directory.");
		try
		{
			codeUpdateInfo = new UpdateInfoImpl(a.updateManagerDirectory);
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create code update info:", ex);
		}
	}
	
	private static void initializeLogging(SplashScreen screen)
	{
		if (screen != null)
			screen.setStatus("Initializing logging.");
		SettingListener listener = new SettingListener() {
			@Override
			public void settingChanged()
			{
				LogWrapper.logToFile(
						Services.settings.logToFile.get() ? Services.settings.logFile.getPath() : null,
						Services.settings.logLength.get());
			}};
		LogWrapper.getLogger().setLevel(Level.INFO);

		settings.logFile.addListener(listener);
		settings.logToFile.addListener(listener);
		settings.logLength.addListener(listener);

//		if (ex instanceof SQLException && Services.notifications != null)
//		{
//			Services.notifications.dbException(ex);
//		}
	}

	private static void startServices(SplashScreen screen)
	{
		if (screen != null)
			screen.setStatus("Starting services");
		notifications.start();
		// Now start other threads...
		checksums.start(); checksums.kick();
		new Thread(connectionServer).start();
		
		downloads.startDownloadInitiator();
		Misc.timer.scheduleAtFixedRate(updateManager, 10000L, 24L * 60L * 60L * 1000L);
		Misc.timer.schedule(h2DbCache, 10 * 60 * 1000);
		startLocalSyncer();
		settings.monitorRepeat.addListener(new SettingListener() {
			@Override
			public void settingChanged() { startLocalSyncer(); }
		});
//		Also need to attempt remote authentications...
	}


	private static void startLocalSyncer()
	{
		synchronized (localSyncerSync)
		{
			if (localSynchronizer != null)
			{
				localSynchronizer.cancel();
			}
			long delay = settings.monitorRepeat.get();
			if (delay < 0)
			{
				localSynchronizer = null;
				return;
			}
			final TimerTask task = new TimerTask() {
				@Override
				public void run()
				{
					UserActions.syncAllLocals(null);
				}};
				Misc.timer.schedule(task, delay, delay);
		}
	}
	
	private static void startSystemTray(SplashScreen screen) throws IOException, AWTException
	{
		if (screen != null)
			screen.setStatus("Starting system tray");
		PopupMenu menu = new PopupMenu();
		MenuItem item = new MenuItem("Show application");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				UserActions.showGui();
			}});
		menu.add(item);
		item = new MenuItem("Show trackers");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				TrackerFrame trackerFrame = new TrackerFrame();
				Services.notifications.registerWindow(trackerFrame);
				trackerFrame.setVisible(true);
			}});
		menu.add(item);
		item = new MenuItem("Quit");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				quiter.quit();
			}});
		menu.add(item);
		item = new MenuItem("Restart");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Main.restart();
			}});
		menu.add(item);
		
		if (SystemTray.isSupported())
		{
			TrayIcon icon = new TrayIcon(Misc.getIcon(), "Convenience Share");
			icon.setPopupMenu(menu);
			SystemTray.getSystemTray().add(icon);
		}
		else
		{
			LogWrapper.getLogger().warning("Your system does not support the System tray.");
			LogWrapper.getLogger().warning("This makes it hard to keep the application running.");
			LogWrapper.getLogger().warning("We will create a JFrame that looks like a SystemTray.");
			new TaskMenu(new ImageIcon(Misc.getIcon()), menu).setVisible(true);
		}
	}

	public static void deInitialize()
	{
		if (updateManager != null)
			updateManager.cancel();
		if (notifications != null)
			notifications.stop();
		if (connectionServer != null)
			connectionServer.stop();
		if (downloads != null)
			downloads.quitAllDownloads();
		Misc.timer.cancel();
		if (checksums != null)
			checksums.quit();
		if (syncs != null)
			syncs.closeAll();
		if (userThreads != null)
			userThreads.shutdownNow();
		if (connectionThreads != null)
			connectionThreads.shutdown();
		if (h2DbCache != null)
			h2DbCache.close();
		if (networkManager != null)
			networkManager.closeAll();
		LogWrapper.close();
	}
	
	public static void testStartUp(SplashScreen screen) throws Exception
	{
		if (screen != null)
			screen.setStatus("Testing startup");
		"foo".getBytes(Settings.encoding);
//		if (!SystemTray.isSupported())
//		{
//			throw new Exception("SystemTray not supported on this OS.");
//		}
		// So far, check ip, check String.getBytes(), check sha1, check encryption, check port
	}


	public static boolean isAlreadyRunning(Arguments args, SplashScreen screen) throws FileNotFoundException, IOException
	{
		quiter = args.quiter;

		settings = args.settings;
		settings.read();
		settings.write();
		settings.listenToSettings();
		initializeLogging(screen);
		
		if (screen != null)
			screen.setStatus("Checking if update manager is running");
		Misc.ensureDirectory(settings.applicationDirectory.get(), false);
		Misc.ensureDirectory(settings.downloadsDirectory.get(), false);
		
		try (Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), settings.servePortBeginI.get());)
		{
			return true;
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect to begin port. Good.\n" + ex.getMessage());
		}
		
		if (screen != null)
			screen.setStatus("Creating log file");
		LogWrapper.logToFile(
				Services.settings.logToFile.get() ? Services.settings.logFile.getPath() : null,
				Services.settings.logLength.get());

		int port = settings.servePortBeginI.get();
		try
		{
			serveSocket = new ServerSocket(port);
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to start on port " + port, ex);
			return true;
		}

		if (screen != null)
			screen.setStatus("Creating database");
		try
		{
			h2DbCache = new DbConnectionCache(args);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to open database ", e);
			return true;
		}
		
		return false;
	}
}
