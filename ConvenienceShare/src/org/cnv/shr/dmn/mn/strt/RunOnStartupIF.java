package org.cnv.shr.dmn.mn.strt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;

interface RunOnStartupIF
{
	void enable();
	void disable();
	Boolean isEnabled();
	String getStartupCmd();


	static LinkedList<String> createStartCmd()
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
		arguments.add(escape(ProcessInfo.getJavaBinary()));
		arguments.add("-jar");
		arguments.add(escape(absolutePath.toString()));
		arguments.add("-f");
		arguments.add(escape(Services.settings.getSettingsFile()));
		arguments.add("-q");
		return arguments;
	}
	static String escape(String filename)
	{
		return "\"" + filename + "\"";
	}
}
