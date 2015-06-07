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
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.dmn.mn.MainTest;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.test.TestActions.TestAction;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;

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
		
		processSettings = new Settings(          Paths.get(root, "settings.props"));
		processSettings.applicationDirectory.set(Paths.get(root, "app"));
		processSettings.downloadsDirectory.set(  Paths.get(root, "downloads"));
		processSettings.setDefaultApplicationDirectoryStructure();
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
		System.out.println("WRITING SETTINGS TO " + processSettings.getSettingsFile());
		processSettings.write();
		
		server = new ServerSocket(0);
		
		LinkedList<String> args = new LinkedList<>();
		
		args.add("java");
		args.add("-cp");
		args.add(ProcessInfo.getTestClassPath());
		// Should be in TestUtils
		args.add("org.cnv.shr.dmn.mn.MainTest");
		args.add(deleteDb ? "-d" : "pass");
		args.add("-k");
		args.add(String.valueOf(10000L));
		args.add("-g");
		args.add("-f");
		args.add(processSettings.getSettingsFile());
		args.add("-t");
		args.add(InetAddress.getLoopbackAddress().getHostAddress());
		args.add(String.valueOf(server.getLocalPort()));
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(args);
		
		builder.directory(ProcessInfo.getJarPath(MainTest.class).toFile());
		process = builder.start();

		try
		{
			Thread.sleep(2000);
		}
		catch (InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
		}
		
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
					LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to close command stream", e);
		}
		try
		{
			if (current != null) current.close();
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close command socket", e);
		}
		try
		{
			if (server != null) server.close();
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close server socket", e);
		}
		try
		{
			if (process != null) process.destroy();
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to destroy process", e);
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
				LogWrapper.getLogger().log(Level.INFO, "Unable to read process stream", e);
			}
		}
	}
}
