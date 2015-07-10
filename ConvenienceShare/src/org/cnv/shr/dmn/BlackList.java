
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



package org.cnv.shr.dmn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class BlackList
{
	private static void ensureExists(Path path)
	{
		Misc.ensureDirectory(path, true);
		if (!Files.exists(path))
		{
			try
			{
				Files.createFile(path);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to create blacklist file at " + path, e);
			}
		}
	}
	
	public synchronized boolean contains(String machineId)
	{
		Path blackListFile = Services.settings.applicationDirectory.getPath().resolve("blackList.txt");
		
		ensureExists(blackListFile);
		Boolean grep = Misc.grep(blackListFile, machineId);
		boolean blackListed = grep != null && grep;
		LogWrapper.getLogger().info(machineId + " is " + (blackListed? "" : "not") + " blacklisted");
		return blackListed;
	}
	public synchronized void add(String machineId)
	{
		Path blackListFile = Services.settings.applicationDirectory.getPath().resolve("blackList.txt");
		Path backup = Paths.get(blackListFile.toString() + ".back");
		ensureExists(blackListFile);
		ensureExists(backup);
		
		LogWrapper.getLogger().info("Blacklisting " + machineId);
		if (Misc.sed(blackListFile, backup, null, null))
		{
			Misc.sed(backup, blackListFile, machineId, null);
		}
	}
	public synchronized void remove(String machineId)
	{
		Path blackListFile = Services.settings.applicationDirectory.getPath().resolve("blackList.txt");
		Path backup = Paths.get(blackListFile.toString() + ".back");
		ensureExists(blackListFile);
		ensureExists(backup);
		
		LogWrapper.getLogger().info("Un-blacklisting " + machineId);
		if (Misc.sed(blackListFile, backup, null, null))
		{
			Misc.sed(backup, blackListFile, null, machineId);
		}
	}

	// TODO: how to do ips?
	public synchronized void setBlacklisted(String machineId, boolean value)
	{
		if (value)
		{
			add(machineId);
		}
		else
		{
			remove(machineId);
		}
	}
}
