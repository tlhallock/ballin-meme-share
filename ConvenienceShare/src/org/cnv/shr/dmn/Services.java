package org.cnv.shr.dmn;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cnv.shr.gui.Application;
import org.cnv.shr.mdl.LocalDirectory;

public class Services
{
	private static final File SETTINGS_FILE = new File("./app/settings.props");
	
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
			settings.read(SETTINGS_FILE);
		}
		catch (IOException e)
		{
			ensureDirectory(SETTINGS_FILE, true);
			settings.write(SETTINGS_FILE);
			
			logger.logStream.println("Creating settings file.");
		}

		ensureDirectory(settings.logLocation, true);
		logger.setLogLocation(settings.logLocation);
		
		ensureDirectory(settings.applicationDirectory, false);
		ensureDirectory(settings.stagingDirectory, false);
		ensureDirectory(settings.downloadsDirectory, false);
		
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
			}
		});
	}

	public static void deInitialize()
	{
		application.dispose();
		
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
	
	private static void ensureDirectory(String path, boolean file)
	{
		ensureDirectory(new File(path), file);
	}
	private static void ensureDirectory(File f, boolean file)
	{
		if (file)
		{
			f = f.getParentFile();
		}
		f.mkdirs();
	}
}
