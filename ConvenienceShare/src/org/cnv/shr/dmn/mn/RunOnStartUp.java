/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cnv.shr.dmn.mn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;



/**
 *
 * @author thallock
 */
public class RunOnStartUp
{
	private static RunOnStartupStatus getStatus(boolean interactive)
	{
		switch (Misc.getOperatingSystem())
		{
		case Linux:
		case Mac:
			return new UnixStatus();
		case Windows:
			return new WindowsStatus();
			default:
				JOptionPane.showMessageDialog(
						Services.notifications.getCurrentContext(),
						"Run on startup is not supported for your Operating System.",
						"OS not supported",
						JOptionPane.WARNING_MESSAGE);
				return null;
		}
	}
	
	public static void runOnStartup()
	{
		LogWrapper.getLogger().info("Enabling run on startup");
		RunOnStartupStatus status = getStatus(true);
		if (!enabledStatusIsAsExpected(status, false))
		{
			return;
		}
		status.enable();
	}
	
	public static void doNotRunOnStartup()
	{
		LogWrapper.getLogger().info("Disabling run on startup");
		RunOnStartupStatus status = getStatus(true);
		if (!enabledStatusIsAsExpected(status, true))
		{
			return;
		}
		status.disable();
	}
	
	public static boolean enabledStatusIsAsExpected(RunOnStartupStatus status, boolean expected)
	{
		if (status == null)
		{
			LogWrapper.getLogger().info("Null status");
			return false;
		}
		Boolean enabled = status.isEnabled();
		if (enabled != null)
		{
			LogWrapper.getLogger().info("Current status is " + enabled);
			return enabled == expected;
		}
			return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
					Services.notifications.getCurrentContext(),
					"ConvenienceShare was not able to see if run on startup is already enabled.\n" + 
					"More information can be found in the logs.\n" +
					"Continue anyway?\n",
					"Unable to determine if ConvenienceShare is enabled.",
					JOptionPane.YES_NO_OPTION);
	}
	
	public static Boolean isEnabled()
	{
		RunOnStartupStatus status = getStatus(true);
		if (status == null)
		{
			LogWrapper.getLogger().info("Null status");
			return null;
		}
		Boolean enabled = status.isEnabled();
		LogWrapper.getLogger().info("Current status is " + enabled);
		return enabled;
	}
	
	private static String createStartCmd()
	{
		Path absolutePath = ProcessInfo.getJarPath(Main.class).toAbsolutePath();
		Path jar = absolutePath.resolve(Misc.CONVENIENCE_SHARE_JAR);
		if (!Files.exists(jar))
		{
			LogWrapper.getLogger().info("Unable to find ConvenienceShare. Should be at " + jar);
			JOptionPane.showMessageDialog(
				Services.notifications.getCurrentContext(),
				"Could not find ConvenienceShare jar at " + jar,
				"Could not find ConvenienceShare.",
				JOptionPane.WARNING_MESSAGE);
			throw new RuntimeException("Unable to find ConvenienceShare");
		}
		
		LinkedList<String> arguments = new LinkedList<>();
		arguments.add(ProcessInfo.getJavaBinary());
		arguments.add("-jar");
		arguments.add(absolutePath.toString());
		arguments.add("-f");
		arguments.add(Services.settings.getSettingsFile());
		return Misc.join(" ", arguments);
	}
	
	private interface RunOnStartupStatus
	{
		void enable();
		void disable();
		Boolean isEnabled();
	}
	
	private static class UnixStatus implements RunOnStartupStatus
	{
		private static final Path profile = findProfile();

		private static Path findProfile()
		{
			for (Path profilePath : new Path[] {
				Paths.get(System.getProperty("user.home")).resolve(".bashrc"),
				Paths.get(System.getProperty("user.home")).resolve(".bash_profile"),
				Paths.get(System.getProperty("user.home")).resolve(".profile"),
			})
			{
				if (Files.exists(profile))
				{
					return profilePath;
				}
			}
			return Paths.get(System.getProperty("user.home")).resolve(".bashrc");
		}

		@Override
		public void enable()
		{
			if (!Files.exists(profile))
			{
				JOptionPane.showMessageDialog(
						Services.notifications.getCurrentContext(),
						"ConvenienceShare was unable to find your profile at\n" +
						profile + ".\n" +
						"This is is required, please either create this file.",
						"Profile not found.",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			String cmd = createStartCmd();

			Path backup = Paths.get(profile.toString() + ".cnv.backup");
			if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
					Services.notifications.getCurrentContext(),
					"ConvenienceShare will run on startup by adding this line:\n" +
					"\"" + cmd + "\"\n" +
					" to \n" + 
					profile + ".\n" +
					"A backup of this file will be made at\n" + 
					backup.toString() + "\n." +
					"Would you like to proceed?",
					"Confirm profile modification",
					JOptionPane.YES_NO_OPTION))
			{
				return;
			}
				
			sed(profile, backup, null, null);
			sed(backup, profile, cmd, null);
		}

		@Override
		public void disable()
		{
			if (!Files.exists(profile))
			{
				JOptionPane.showMessageDialog(
						Services.notifications.getCurrentContext(),
						"ConvenienceShare was unable to find your profile at\n" +
						profile + ".\n" +
						"This is is required, please either create this file.",
						"Profile not found.",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			String cmd = createStartCmd();

			Path backup = Paths.get(profile.toString() + ".cnv.backup");
			if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
					Services.notifications.getCurrentContext(),
					"ConvenienceShare will disable run on startup by removing this line:\n" +
					"\"" + cmd + "\"\n" +
					" from \n" + 
					profile + ".\n" +
					"A backup of this file will be made at\n" + 
					backup.toString() + "\n." +
					"Would you like to proceed?",
					"Confirm profile modification",
					JOptionPane.YES_NO_OPTION))
			{
				return;
			}
				
			sed(profile, backup, null, null);
			sed(backup, profile, null, cmd);
		}

		@Override
		public Boolean isEnabled()
		{
			return Misc.grep(profile, createStartCmd());
		}
	}

	
	private static class WindowsStatus implements RunOnStartupStatus
	{
		private static final Path launchScript = Paths.get(System.getProperty("user.home")).resolve("myTest.bat");

		@Override
		public void enable()
		{
			Misc.ensureDirectory(launchScript, true);
			LogWrapper.getLogger().info("Creating startup script at " + launchScript);
			try (BufferedWriter writer = Files.newBufferedWriter(launchScript);)
			{
				writer.write(createStartCmd());
			}
			catch (IOException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to write to " + launchScript, ex);
				return;
			}
		}

		@Override
		public void disable()
		{
			try
			{
				LogWrapper.getLogger().info("Deleting startup script at " + launchScript);
				Files.delete(launchScript);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(
						Services.notifications.getCurrentContext(),
						"ConvenienceShare was unable to remove\n" +
						launchScript + ".\n",
						"Unable to remove launch script.",
						JOptionPane.WARNING_MESSAGE);
			}
//			try
//			{
//				Files.delete(launchScript.getParent());
//			}
//			catch (IOException e)
//			{
//				JOptionPane.showMessageDialog(
//						Services.notifications.getCurrentContext(),
//						"ConvenienceShare was unable to remove\n" +
//						launchScript + ".\n",
//						"Unable to remove launch script.",
//						JOptionPane.WARNING_MESSAGE);
//			}
		}

		@Override
		public Boolean isEnabled()
		{
			return Files.exists(launchScript);
		}
	}

	private static void sed(Path p, Path backup, String toInsert, String toRemove)
	{
		try (BufferedReader reader = Files.newBufferedReader(p); BufferedWriter writer = Files.newBufferedWriter(backup);)
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (toRemove != null && line.equals(toRemove))
				{
					continue;
				}
				writer.write(line);
				writer.write("\n");
			}

			if (toInsert != null)
			{
				writer.write(toInsert);
				writer.write("\n");
			}
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to insert " + toInsert + " into " + p, ex);
			return;
		}
	}
}
