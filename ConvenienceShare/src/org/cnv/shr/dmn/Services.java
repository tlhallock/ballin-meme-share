package org.cnv.shr.dmn;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cnv.shr.gui.Application;
import org.cnv.shr.util.Misc;

public class Services
{	
	public static ExecutorService userThreads;
	public static ExecutorService requestThreads;
	public static ExecutorService serveThreads;
	
	public static Settings settings;
	public static Logger logger;
	public static ChecksumManager checksums;
	public static NetworkQueue requestQueue;
	public static RequestHandler handler;
	public static Remotes remotes;
	public static Locals locals;

	public static Timer monitorTimer;

	public static Application application;
	
	public static void initialize() throws IOException
	{
		logger = new Logger();
		settings = new Settings();
		settings.setToDefaults();
		
		try
		{
			settings.read();
		}
		catch (IOException e)
		{
			settings.write();
			logger.logStream.println("Creating settings file.");
		}

		logger.setLogLocation();
		
		Misc.ensureDirectory(settings.applicationDirectory, false);
		Misc.ensureDirectory(settings.stagingDirectory, false);
		Misc.ensureDirectory(settings.downloadsDirectory, false);
		
		userThreads     = Executors.newCachedThreadPool();
		requestThreads  = Executors.newCachedThreadPool();
		serveThreads    = Executors.newCachedThreadPool();
		
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

		java.awt.EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				application = new Application();
				application.refreshAll();
				application.setVisible(true);
				
				locals.read();
				remotes.read();
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
		requestThreads.shutdown();
		serveThreads.shutdown();
		
		try
		{
			userThreads.awaitTermination(60, TimeUnit.SECONDS);
			requestThreads.awaitTermination(60, TimeUnit.SECONDS);
			serveThreads.awaitTermination(60, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		logger.close();
	}
}
