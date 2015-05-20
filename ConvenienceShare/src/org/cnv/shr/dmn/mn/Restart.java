package org.cnv.shr.dmn.mn;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;

public class Restart extends Quiter
{
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
			args.add("-cp");
			args.add(Misc.getClassPath());
			args.add(Misc.getJarName());
			args.add("-f");
			args.add(Services.settings.getSettingsFile().getAbsolutePath());
			
			System.out.println("Restarting from:");
			System.out.println(new File(".").getAbsolutePath());
			System.out.println("with:");
			for (String str : args)
			{
				System.out.println(str);
			}

			ProcessBuilder builder = new ProcessBuilder();
			builder.command(args);
			builder.directory(new File("./bin").getAbsoluteFile());

			builder.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			System.exit(0);
		}
	}
}
