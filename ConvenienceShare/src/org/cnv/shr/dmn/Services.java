package org.cnv.shr.dmn;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cnv.shr.db.DbConnection;
import org.cnv.shr.gui.Application;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.MessageReader;
import org.cnv.shr.util.Misc;

public class Services
{
	/** To take items off the AWT event thread **/
	public static ExecutorService userThreads;
	/** To handle network traffic **/
	public static ExecutorService connectionThreads;
	
	public static Settings settings;
	public static Logger logger;
	public static Notifications notifications;
	public static ChecksumManager checksums;
	public static ConnectionManager networkManager;
	public static RequestHandler handler;
	public static Remotes remotes;
	public static Locals locals;
	public static MessageReader msgReader;
	public static KeyManager keyManager;
	public static DbConnection db;

	public static Timer monitorTimer;

	public static Application application;
	
	public static Machine localMachine;
	
	public static void initialize(String[] args) throws Exception
	{
		logger = new Logger();
		
		if (args.length >= 1)
		{
			settings = new Settings(new File(args[0]));
		}
		else
		{
			settings = new Settings(new File("convencie_share_settings.props"));
		}
		
		settings.setToDefaults();
		
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
		
		db = new DbConnection();

		keyManager = new KeyManager();
		localMachine = new Machine.LocalMachine();

		networkManager = new ConnectionManager();
		msgReader = new MessageReader();
		
		Misc.ensureDirectory(settings.applicationDirectory, false);
		Misc.ensureDirectory(settings.stagingDirectory, false);
		Misc.ensureDirectory(settings.downloadsDirectory, false);
		
		userThreads     = Executors.newCachedThreadPool();
		connectionThreads  = Executors.newCachedThreadPool();
		
		locals = new Locals();
		remotes = new Remotes();
		checksums = new ChecksumManager();
		handler = new RequestHandler();
		monitorTimer = new Timer();

		
		checksums.start();
		handler.start();
		monitorTimer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				locals.synchronize();
				
			}}, settings.monitorRepeat, settings.monitorRepeat);
		
		// Ensure the local machine is added and that the downloads directory is shared.
		db.addMachine(localMachine);
		locals.share(new File(settings.downloadsDirectory));

		java.awt.EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					application = new Application();
					application.refreshAll();
					application.setVisible(true);
					
					locals.synchronize();
					remotes.refresh();
				}
				catch (Exception ex)
				{
					Services.logger.logStream.println("Unable to start GUI.\nQuiting.");
					ex.printStackTrace(Services.logger.logStream);
					Main.quit();
				}
			}
		});
	}

	public static void deInitialize()
	{
		if (application != null)
		{
			application.dispose();
		}
		
		handler.quit();
		
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
		
		db.close();

		logger.close();
	}
}
