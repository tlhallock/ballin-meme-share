package org.cnv.shr.dmn;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.cnv.shr.db.DbConnection;
import org.cnv.shr.db.h2.DbConnectionCache;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.gui.Application;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.MessageReader;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.Misc;

public class Services
{
	/** To take items off the AWT event thread **/
	public static ExecutorService userThreads;
	/** To handle outgoing network traffic **/
	public static ExecutorService connectionThreads;
	public static Settings settings;
	public static Logger logger;
	/** delete this **/
	public static Notifications notifications;
	public static ChecksumManager checksums;
	public static ConnectionManager networkManager;
	public static RequestHandler[] handlers;
	public static Remotes remotes;
	public static Locals locals;
	public static MessageReader msgReader;
	public static KeyManager keyManager;
	public static DbConnection db;
	public static Timer monitorTimer;
	public static Application application;
	public static Machine localMachine;
	public static DbConnectionCache h2DbCache;
	
	public static void initialize(String[] args) throws Exception
	{
		logger = new Logger();
		settings = new Settings(new File(args.length >= 1 ? args[0] : "convencie_share_settings.props"));
		try
		{
			settings.read();
		}
		catch (IOException e)
		{
			logger.logStream.println("Creating settings file.");
		}
		settings.write();

		logger.setLogLocation();
		notifications = new Notifications();
		h2DbCache = new DbConnectionCache();
		keyManager = new KeyManager();
		localMachine = new Machine.LocalMachine();
		networkManager = new ConnectionManager();
		msgReader = new MessageReader();
		
		Misc.ensureDirectory(settings.applicationDirectory.get(), false);
		Misc.ensureDirectory(settings.stagingDirectory.get(), false);
		Misc.ensureDirectory(settings.downloadsDirectory.get(), false);
		
		userThreads        = Executors.newCachedThreadPool();
		connectionThreads  = new ThreadPoolExecutor(0, settings.maxDownloads.get(), 
				60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		checksums = new ChecksumManager();
		locals = new Locals();
		remotes = new Remotes();
		int numServeThreads = Math.min(1, settings.maxServes.get());
		handlers = new RequestHandler[numServeThreads];
		for (int i = 0; i < handlers.length; i++)
		{
			handlers[i] = new RequestHandler(settings.servePortBegin.get() + i);
		}
		monitorTimer = new Timer();

		
		checksums.start();
		for (int i = 0; i < handlers.length; i++)
		{
			handlers[i].start();
		}
		monitorTimer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				locals.synchronize(false);
			}}, settings.monitorRepeat.get(), settings.monitorRepeat.get());
		
		// Ensure the local machine is added and that the downloads directory is shared.
		localMachine.update();
		locals.share(settings.downloadsDirectory.get());

		java.awt.EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					application = new Application();
					application.setVisible(true);
					application.refreshAll();

//					application.refreshLocals(DbRoots.listLocals(null));
//					application.refreshRemotes(Services.db.getRemoteMachines());
				}
				catch (Exception ex)
				{
					Services.logger.logStream.println("Unable to start GUI.\nQuiting.");
					ex.printStackTrace(Services.logger.logStream);
					Main.quit();
				}
			}
		});
		
		monitorTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				remotes.refresh();
			}}, 1000);
	}

	public static void deInitialize()
	{
		if (application != null)
		{
			application.dispose();
		}

		for (int i = 0; i < handlers.length; i++)
		{
			handlers[i].quit();
		}
		
		monitorTimer.cancel();
		checksums.quit();
		
		userThreads.shutdown();
		connectionThreads.shutdown();
		
		try
		{
			userThreads.awaitTermination(60, TimeUnit.SECONDS);
			connectionThreads.awaitTermination(60, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			Services.logger.logStream.println("Error closing thread pools.");
			e.printStackTrace(Services.logger.logStream);
		}
		
//		db.close();

		logger.close();
	}
	
	public static void testStartUp()
	{
		// So far, check ip, check String.getBytes(), check sha1, check encryption, check port
	}
}
