package org.cnv.shr.dmn.mn.strt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;

class WindowsStartup implements RunOnStartupIF
{
	private static final Path launchScript = Paths.get(System.getProperty("user.home")).resolve("myTest.bat");

	@Override
	public void enable()
	{
		Misc.ensureDirectory(launchScript, true);
		LogWrapper.getLogger().info("Creating startup script at " + launchScript);
		try (BufferedWriter writer = Files.newBufferedWriter(launchScript);)
		{
			writer.write(getStartupCmd());
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
//		try
//		{
//			Files.delete(launchScript.getParent());
//		}
//		catch (IOException e)
//		{
//			JOptionPane.showMessageDialog(
//					Services.notifications.getCurrentContext(),
//					"ConvenienceShare was unable to remove\n" +
//					launchScript + ".\n",
//					"Unable to remove launch script.",
//					JOptionPane.WARNING_MESSAGE);
//		}
	}

	@Override
	public Boolean isEnabled()
	{
		return Files.exists(launchScript);
	}

	@Override
	public String getStartupCmd()
	{
		LinkedList<String> arguments = RunOnStartupIF.createStartCmd();
		String join = Misc.join(" ", arguments);
		return "dir " + RunOnStartupIF.escape(ProcessInfo.getJarPath(Main.class).toAbsolutePath().toString()) + " & " + join;
	}

}
