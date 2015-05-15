package org.cnv.shr.dmn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.cnctn.ConnectionManager;
import org.cnv.shr.db.h2.DbConnectionCache;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.dwn.DownloadManager;
import org.cnv.shr.dmn.dwn.ServeManager;
import org.cnv.shr.gui.Application;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.Machine.LocalMachine;
import org.cnv.shr.msg.MessageReader;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.sync.RemoteSynchronizers;
import org.cnv.shr.util.Misc;

public class Services
{
	/** To take items off the AWT event thread **/
	public static ExecutorService userThreads;
	/** To handle outgoing network traffic **/
	public static ExecutorService connectionThreads;
	/** To handle incoming network traffic **/
	public static ExecutorService serveThreads;
	public static Settings settings;
	public static Logger logger;
	/** delete this **/
	public static Notifications notifications;
	public static ChecksumManager checksums;
	public static ConnectionManager networkManager;
	public static RequestHandler[] handlers;
	public static MessageReader msgReader;
	public static KeyManager keyManager;
	public static Timer monitorTimer;
	public static Application application;
	public static LocalMachine localMachine;
	public static DbConnectionCache h2DbCache;
	public static ServeManager server;
	public static DownloadManager downloads;
	public static RemoteSynchronizers syncs;
	public static BlackList blackList;
	
	public static void initialize(Settings stgs, boolean deleteDb) throws Exception
	{
		createServices(stgs, deleteDb);
		testStartUp();
		startServices();
	}
	private static void createServices(Settings stgs, boolean deleteDb) throws FileNotFoundException, IOException, SQLException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, CertificateEncodingException, InvalidKeySpecException
	{
		settings = stgs;
		logger = new Logger();
		settings.write();
		settings.listenToSettings();

		logger.setLogLocation();
		h2DbCache = new DbConnectionCache(deleteDb);
		notifications = new Notifications();
		keyManager = new KeyManager(settings.keysFile.get());
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

		int numServeThreads = Math.min(1, settings.maxServes.get());
		handlers = new RequestHandler[numServeThreads];
		for (int i = 0; i < handlers.length; i++)
		{
			int port = settings.servePortBegin.get() + i;
			handlers[i] = new RequestHandler(new ServerSocket(port));
		}
		
		userThreads        = Executors.newCachedThreadPool();
		connectionThreads  = new ThreadPoolExecutor(0, settings.maxDownloads.get(), 
				60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		serveThreads       = Executors.newFixedThreadPool(numServeThreads);
		checksums = new ChecksumManager();
	}

	private static void startServices()
	{
		// Now start other threads...
		checksums.start();
		for (int i = 0; i < handlers.length; i++)
		{
			serveThreads.execute(handlers[i]);
		}
		
		monitorTimer = new Timer();
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
		
		java.awt.EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					application = new Application();
					application.setVisible(true);
					application.refreshAll();

					application.refreshLocals();
					application.refreshRemotes();
				}
				catch (Exception ex)
				{
					Services.logger.println("Unable to start GUI.\nQuiting.");
					Services.logger.print(ex);
					Main.quit();
				}
			}
		});
	}

	public static void deInitialize()
	{
		for (int i = 0; i < handlers.length; i++)
		{
			if (handlers[i] != null)
				handlers[i].quit();
		}
		if (application != null)
			application.dispose();
		if (monitorTimer != null)
			monitorTimer.cancel();
		if (checksums != null)
			checksums.quit();
		if (syncs != null)
			syncs.closeAll();
		if (userThreads != null)
			userThreads.shutdownNow();
		if (connectionThreads != null)
			connectionThreads.shutdownNow();
		if (serveThreads != null)
			serveThreads.shutdownNow();
		if (h2DbCache != null)
			h2DbCache.close();
		if (networkManager != null)
			networkManager.closeAll();
		if (logger != null)
			logger.close();
	}
	
	public static void testStartUp() throws UnsupportedEncodingException
	{
		"foo".getBytes(Settings.encoding);
		// So far, check ip, check String.getBytes(), check sha1, check encryption, check port
	}
}
