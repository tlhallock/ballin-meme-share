package org.cnv.shr.dmn;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.swing.ImageIcon;

import org.cnv.shr.cnctn.ConnectionManager;
import org.cnv.shr.db.h2.DbConnectionCache;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.dwn.DownloadManager;
import org.cnv.shr.dmn.dwn.ServeManager;
import org.cnv.shr.dmn.mn.Arguments;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.dmn.mn.Quiter;
import org.cnv.shr.gui.TaskMenu;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.Machine.LocalMachine;
import org.cnv.shr.msg.MessageReader;
import org.cnv.shr.stng.SettingListener;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.sync.RemoteSynchronizers;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class Services
{
	/** To take items off the AWT event thread **/
	public static ExecutorService userThreads;
	/** To handle outgoing network traffic **/
	public static ExecutorService connectionThreads;
	/** To handle incoming network traffic **/
	public static RequestHandler[] handlers;
	public static Settings settings;
	/** delete this **/
	public static Notifications notifications;
	public static ChecksumManager checksums;
	public static ConnectionManager networkManager;
	public static MessageReader msgReader;
	public static KeysService keyManager;
	public static Timer timer;
	public static LocalMachine localMachine;
	public static DbConnectionCache h2DbCache;
	public static ServeManager server;
	public static DownloadManager downloads;
	public static RemoteSynchronizers syncs;
	public static BlackList blackList;
	public static Quiter quiter;
	
	public static void initialize(Arguments args) throws Exception
	{
		quiter = args.quiter;
		args.settings.read();
		createServices(args.settings, args.deleteDb);
		testStartUp();
		startServices();
	}
	
	
	private static void createServices(Settings stgs, boolean deleteDb) throws Exception
	{
		settings = stgs;
		settings.write();
		settings.listenToSettings();
		initializeLogging();
		
		h2DbCache = new DbConnectionCache(deleteDb);
        
		notifications = new Notifications();
		keyManager = new KeysService();
		keyManager.readKeys();
		localMachine = new Machine.LocalMachine();
		if (!localMachine.save())
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
		
		Misc.ensureDirectory(settings.applicationDirectory.get(), false);
		Misc.ensureDirectory(settings.stagingDirectory.get(), false);
		Misc.ensureDirectory(settings.downloadsDirectory.get(), false);

		int numServeThreads = Math.max(1, settings.maxServes.get());
		handlers = new RequestHandler[numServeThreads];
		int successCount = 0;
		for (int i = 0; i < handlers.length; i++)
		{
			int port = settings.servePortBeginI.get() + i;
			try
			{
				handlers[i] = new RequestHandler(port);
				successCount++;
			}
			catch (IOException ex)
			{
				LogWrapper.getLogger().log(Level.WARNING, "Unable to start on port " + port, ex);
			}
		}
		if (successCount <= 0)
		{
			throw new Exception("Unable to start serve threads!");
		}
		
		userThreads        = Executors.newCachedThreadPool();
		connectionThreads  = new ThreadPoolExecutor(0, settings.maxDownloads.get(), 
				60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		checksums = new ChecksumManager();
		
		startSystemTray();
	}
	
	private static void initializeLogging()
	{
//		LogWrapper.initialize();
		SettingListener listener = new SettingListener() {
			@Override
			public void settingChanged()
			{
				LogWrapper.logToFile(
						Services.settings.logToFile.get() ? Services.settings.logFile.get() : null,
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

	private static void startServices()
	{
		notifications.start();
		// Now start other threads...
		checksums.start();
		for (int i = 0; i < handlers.length; i++)
		{
			if (handlers[i] != null)
			handlers[i].start();
		}
		
		timer = new Timer();
		downloads.initiatePendingDownloads();
//		monitorTimer.schedule(new TimerTask() {
//			@Override
//			public void run()
//			{
//				locals.synchronize(false);
//			}}, settings.monitorRepeat.get(), settings.monitorRepeat.get());
//		
//		monitorTimer.schedule(new TimerTask() {
//			@Override
//			public void run() {
//				remotes.refresh();
//			}}, 1000);
		
//		Also need to attempt remote authentications...
		
		UserActions.showGui();
	}
	
	private static void startSystemTray() throws IOException, AWTException
	{
		PopupMenu menu = new PopupMenu();
		MenuItem item = new MenuItem("Show");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				UserActions.showGui();
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
		if (notifications != null)
			notifications.stop();
		if (handlers != null)
		for (int i = 0; i < handlers.length; i++)
		{
			if (handlers[i] != null)
				handlers[i].quit();
		}
		if (downloads != null)
			downloads.quitAllDownloads();
		if (timer != null)
			timer.cancel();
		if (checksums != null)
			checksums.quit();
		if (syncs != null)
			syncs.closeAll();
		if (userThreads != null)
			userThreads.shutdownNow();
		if (connectionThreads != null)
			connectionThreads.shutdownNow();
		if (h2DbCache != null)
			h2DbCache.close();
		if (networkManager != null)
			networkManager.closeAll();
	}
	
	public static void testStartUp() throws Exception
	{
		"foo".getBytes(Settings.encoding);
//		if (!SystemTray.isSupported())
//		{
//			throw new Exception("SystemTray not supported on this OS.");
//		}
		// So far, check ip, check String.getBytes(), check sha1, check encryption, check port
	}
}
