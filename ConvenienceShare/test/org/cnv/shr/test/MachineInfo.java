package org.cnv.shr.test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.test.TestActions.TestAction;
import org.cnv.shr.util.Misc;

public class MachineInfo
{
	private String name;
	private Settings processSettings;
	private Process process;
	
	private ServerSocket server;
	private Socket current;
	private ObjectOutputStream commandStream;
	
	private String url;
	
	public MachineInfo(String root, int port, String id) throws IOException
	{
		name = id;
		File rootDirectory = new File(root);
		rootDirectory.mkdirs();
		
		Misc.ensureDirectory(root, false);
		root = new File(root).getCanonicalPath();
		
		processSettings = new Settings(          new File(root + File.separator + "settings.props"));
		processSettings.applicationDirectory.set(new File(root + File.separator + "app"));
		processSettings.logFile.set(             new File(root + File.separator + "app" + File.separator + "log.txt"));
		processSettings.keysFile.set(            new File(root + File.separator + "app" + File.separator + "keys.txt"));
		processSettings.dbFile.set(              new File(root + File.separator + "app" + File.separator + "files_database"));
		processSettings.downloadsDirectory.set(  new File(root + File.separator + "downloads"));
		processSettings.servingDirectory.set(    new File(root + File.separator + "serve"));
		processSettings.stagingDirectory.set(    new File(root + File.separator + "stage"));
		processSettings.shareWithEveryone.set(true);
		processSettings.servePortBeginI.set(port);
		processSettings.servePortBeginE.set(port);
		processSettings.machineIdentifier.set(id);
		
		url = InetAddress.getLoopbackAddress().getHostAddress() + ":" + port;
	}
	
	public String getUrl()
	{
		return url;
	}

	public String getIdent()
	{
		return name;
	}
	
	public void send(TestAction action) throws IOException
	{
		commandStream.writeObject(action);
		commandStream.flush();
	}
	
	public Closeable launch(boolean deleteDb) throws IOException
	{
		System.out.println("WRITING SETTINGS TO " + processSettings.getSettingsFile().getCanonicalPath());
		processSettings.write();
		
		server = new ServerSocket(0);
		
		String[] args = new String[]
		{
				TestUtils.getJavaPath(),
				"-cp",
				TestUtils.getClassPath(),
				"org.cnv.shr.dmn.mn.MainTest",
				deleteDb ? "-d" : "pass",
				"-k",  String.valueOf(10000L),
				"-f",
				processSettings.getSettingsFile().getAbsolutePath(),
				"-t",
				InetAddress.getLoopbackAddress().getHostAddress(), String.valueOf(server.getLocalPort()),
		};
		process = Runtime.getRuntime().exec(args, null, new File(TestUtils.getJarPath()));

		try
		{
			Thread.sleep(2000);
		}
		catch (InterruptedException e)
		{
			Services.logger.print(e);
		}

		System.out.println(Arrays.toString(args));
		System.out.println("From: " + TestUtils.getJarPath());
		
		new OutputThread(name, System.err, new BufferedReader(new InputStreamReader(process.getErrorStream()))).start();
		new OutputThread(name, System.out, new BufferedReader(new InputStreamReader(process.getInputStream()))).start();
		
		current = server.accept();
		commandStream = new ObjectOutputStream(current.getOutputStream());
		
		return new Closeable() {
			@Override
			public void close() throws IOException
			{
				send(new TestActions.Die());
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					Services.logger.print(e);
				}
				kill();
			}};
	}

	public void kill()
	{
		try
		{
			if (commandStream != null) commandStream.close();
		}
		catch (Exception e)
		{
			Services.logger.print(e);
		}
		try
		{
			if (current != null) current.close();
		}
		catch (Exception e)
		{
			Services.logger.print(e);
		}
		try
		{
			if (server != null) server.close();
		}
		catch (Exception e)
		{
			Services.logger.print(e);
		}
		try
		{
			if (process != null) process.destroy();
		}
		catch (Exception e)
		{
			Services.logger.print(e);
		}
	}

	private static class OutputThread extends Thread
	{
		private PrintStream out;
		private BufferedReader reader;
		private String name;
		
		public OutputThread(String name, PrintStream out, BufferedReader reader)
		{
			this.name = name;
			this.out = out;
			this.reader = reader;
		}
		
		@Override
		public void run()
		{
			try
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					out.println(name + ":" + line);
				}
				System.out.println("Process done.");
			}
			catch (IOException e)
			{
				Services.logger.print(e);
			}
		}
	}
}
