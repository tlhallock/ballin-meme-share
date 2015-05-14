package org.cnv.shr.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.Misc;

public class MachineInfo
{
	private String name;
	private Settings processSettings;
	private Process process;
	
	public MachineInfo(String root, int port, String id) throws UnknownHostException
	{
		name = id;
		File rootDirectory = new File(root);
		rootDirectory.mkdirs();
		
		processSettings = new Settings(          new File(root + File.separator + "settings.props"));
		processSettings.applicationDirectory.set(new File(root + File.separator + "app"));
		processSettings.logFile.set(             new File(root + File.separator + "app" + File.separator + "log.txt"));
		processSettings.keysFile.set(            new File(root + File.separator + "app" + File.separator + "keys.txt"));
		processSettings.dbFile.set(              new File(root + File.separator + "app" + File.separator + "files_database"));
		processSettings.downloadsDirectory.set(  new File(root + File.separator + "downloads"));
		processSettings.servingDirectory.set(    new File(root + File.separator + "serve"));
		processSettings.stagingDirectory.set(    new File(root + File.separator + "stage"));
		processSettings.servePortBegin.set(port);
		processSettings.machineIdentifier.set(id);
	}
	
	public ServerSocket launch() throws IOException
	{
		processSettings.write();
		
		ServerSocket socket = new ServerSocket(0);
		
		String[] args = new String[]
		{
				Misc.getJavaPath(),
				"-cp",
				Misc.getClassPath(),
				Misc.getJarName(),
				"-d",
				"-k",  String.valueOf(10000L),
				"-f",
				processSettings.getSettingsFile().getAbsolutePath(),
				"-t",
				InetAddress.getLoopbackAddress().getHostAddress(), String.valueOf(socket.getLocalPort()),
		};
		process = Runtime.getRuntime().exec(args, null, new File(Misc.getJarPath()));

		System.out.println(Arrays.toString(args));
		System.out.println("From: " + Misc.getJarPath());
		
		new OutputThread(name, System.err, new BufferedReader(new InputStreamReader(process.getErrorStream()))).start();
		new OutputThread(name, System.out, new BufferedReader(new InputStreamReader(process.getInputStream()))).start();
		
		return socket;
	}
	
	public void kill()
	{
		if (process != null)
		{
			process.destroy();
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
				e.printStackTrace();
			}
		}
	}
}
