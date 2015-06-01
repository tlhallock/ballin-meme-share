package org.cnv.shr.dmn.mn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.ProcessInfo;

public class Restart extends Quiter
{
	private Path launchDir;
	
	public Restart() { this (ProcessInfo.getJarFile(Main.class).getParent()); }
	
	public Restart(Path launch) { this.launchDir = launch; }
	
	@Override
	public void doFinal()
	{
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}
		try
		{
			LinkedList<String> args = new LinkedList<>();
			args.add("java");
			args.add("-jar");
			args.add(ProcessInfo.getJarFile(Main.class).toString());
			args.add("-f");
			args.add(Services.settings.getSettingsFile());
			
			System.out.println("Restarting from:");
			System.out.println(launchDir.toString());
			System.out.println("with:");
			for (String str : args)
			{
				System.out.println(str);
			}

			ProcessBuilder builder = new ProcessBuilder();
			builder.command(args);
			builder.directory(launchDir.toFile());

			builder.start();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to start new process.", e);
		}
		finally
		{
			System.exit(0);
		}
	}
}
