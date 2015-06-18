
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.dmn.mn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.ProcessInfo;

public class Restart extends Quiter
{
	private Path launchDir;
	private LinkedList<String> arguments;
	
	public Restart() { this (ProcessInfo.getJarPath(Main.class)); }
	
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
			args.add(launchDir.resolve(Misc.CONVENIENCE_SHARE_JAR).toString());
			args.add("-f");
			args.add(Services.settings.getSettingsFile());
			args.addAll(arguments);
			
			LogWrapper.getLogger().info("Restarting from:");
			LogWrapper.getLogger().info(launchDir.toString());
			LogWrapper.getLogger().info("with:");
			for (String str : args)
			{
				LogWrapper.getLogger().info(str);
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

	public void setArgs(LinkedList<String> args)
	{
		this.arguments = args;
	}
}
