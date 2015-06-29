/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cnv.shr.dmn.mn.strt;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;



/**
 *
 * @author thallock
 */
public class RunOnStartUp
{
	private static RunOnStartupIF getStartupImpl(boolean interactive)
	{
		switch (Misc.getOperatingSystem())
		{
		case Linux:
		case Mac:
			return new UnixStartup();
		case Windows:
			return new WindowsStartup();
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
		RunOnStartupIF status = getStartupImpl(true);
		if (!enabledStatusIsAsExpected(status, false))
		{
			return;
		}
		status.enable();
	}
	
	public static void doNotRunOnStartup()
	{
		LogWrapper.getLogger().info("Disabling run on startup");
		RunOnStartupIF status = getStartupImpl(true);
		if (!enabledStatusIsAsExpected(status, true))
		{
			return;
		}
		status.disable();
	}
	
	public static boolean enabledStatusIsAsExpected(RunOnStartupIF status, boolean expected)
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
		RunOnStartupIF status = getStartupImpl(true);
		if (status == null)
		{
			LogWrapper.getLogger().info("Null status");
			return null;
		}
		Boolean enabled = status.isEnabled();
		LogWrapper.getLogger().info("Current status is " + enabled);
		return enabled;
	}
}
