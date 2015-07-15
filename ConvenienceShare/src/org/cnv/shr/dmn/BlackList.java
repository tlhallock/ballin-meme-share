
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.logging.Level;

import org.cnv.shr.stng.SettingListener;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class BlackList implements SettingListener
{
	private HashSet<String> lines = new HashSet<>();
	private long timeStamp;
	
	public BlackList()
	{
		read();
		Services.settings.blackListFile.addListener(this);
	}
	
	private static void ensureExists(Path path)
	{
		Misc.ensureDirectory(path, true);
		if (Files.exists(path))
		{
			return;
		}
		try
		{
			Files.createFile(path);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create blacklist file at " + path, e);
		}
	}

	public synchronized boolean contains(String machineId)
	{
		checkTime();
		return lines.contains(machineId);
	}

	public synchronized void add(String machineId)
	{
		lines.add(machineId);
		write();
	}

	public synchronized void remove(String machineId)
	{
		lines.remove(machineId);
		write();
	}

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
	
	private void checkTime()
	{
		Path blackListFile = Services.settings.blackListFile.getPath();
		try
		{
			if (timeStamp < Files.getLastModifiedTime(blackListFile).toMillis())
			{
				read();
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get modified time of blacklist file: " + blackListFile, e);
		}
	}
	
	private synchronized void read()
	{
		timeStamp = System.currentTimeMillis();
		Path blackListFile = Services.settings.blackListFile.getPath();
		ensureExists(blackListFile);
		try (BufferedReader reader = Files.newBufferedReader(blackListFile);)
		{
			lines.clear();
			String line;
			while ((line = reader.readLine()) != null)
			{
				lines.add(line);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read blacklist file at " + blackListFile, e);
			write();
		}
	}
	private synchronized void write()
	{
		Path blackListFile = Services.settings.blackListFile.getPath();
		ensureExists(blackListFile);
		try (BufferedWriter writer = Files.newBufferedWriter(blackListFile);)
		{
			for (String str : lines)
			{
				writer.write(str);
				writer.write("\n");
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write blacklist file at " + blackListFile, e);
		}
	}

	@Override
	public void settingChanged()
	{
		read();
		write();
	}
}
