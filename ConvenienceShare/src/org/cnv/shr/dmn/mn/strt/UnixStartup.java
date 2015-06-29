package org.cnv.shr.dmn.mn.strt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;

class UnixStartup implements RunOnStartupIF
{
	private static final Path profile = findProfile();

	private static Path findProfile()
	{
		/**
		 * TODO: The bashrc is really not the best place for this!!!
		 */
		for (Path profilePath : new Path[] {
			Paths.get(System.getProperty("user.home")).resolve(".bashrc"),
			Paths.get(System.getProperty("user.home")).resolve(".bash_profile"),
			Paths.get(System.getProperty("user.home")).resolve(".profile"),
		})
		{
			if (Files.exists(profilePath))
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
		
		String cmd = getStartupCmd();

		Path backup = Paths.get(profile.toString() + ".cnv.backup");
		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
				Services.notifications.getCurrentContext(),
				"ConvenienceShare will run on startup by adding this line:\n" +
				"\"" + cmd + "\"\n" +
				" to \n" + 
				profile + ".\n" +
				"A backup of this file will be made at\n" + 
				backup.toString() + ".\n" +
				"Would you like to proceed?",
				"Confirm profile modification",
				JOptionPane.YES_NO_OPTION))
		{
			return;
		}
		
		if (Misc.sed(profile, backup, null, null))
		{
			Misc.sed(backup, profile, cmd, null);
		}
		else
		{
			// log
		}
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
		
		String cmd = getStartupCmd();

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
			
		if (Misc.sed(profile, backup, null, null))
		{
			Misc.sed(backup, profile, null, cmd);
		}
		else
		{
			// log
		}
	}

	@Override
	public Boolean isEnabled()
	{
		return Misc.grep(profile, getStartupCmd());
	}

	@Override
	public String getStartupCmd()
	{
		LinkedList<String> arguments = RunOnStartupIF.createStartCmd();
		String join = Misc.join(" ", arguments);
		return "( cd " + RunOnStartupIF.escape(ProcessInfo.getJarPath(Main.class).toAbsolutePath().toString()) + " ; " + join + " )";
	}
}
