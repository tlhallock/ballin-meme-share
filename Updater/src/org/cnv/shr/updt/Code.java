
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


package org.cnv.shr.updt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.zip.ZipException;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.ProcessInfo;

public class Code
{
	private long timeStamp;
	private Path jar = Updater.getUpdatesDirectory().resolve("ConvenienceShare.jar");
	private String version;
	
	public Code() throws ZipException, IOException
	{
		checkTime();
		// Add watch service?
	}
	
	public void checkTime()
	{
		long fsTime;
		try
		{
			fsTime = Files.getLastModifiedTime(jar).toMillis();
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get fs time.", e1);
			return;
		}

		if (timeStamp > fsTime)
		{
			return;
		}
		
		version = ProcessInfo.getJarVersion(jar);
		LogWrapper.getLogger().info("read version " + version + " from jar.");
		if (version != null)
		{
			timeStamp = fsTime;
		}
	}
	
	public String getVersion()
	{
		checkTime();
		return version;
	}

	public InputStream getStream() throws IOException
	{
		checkTime();
		return Files.newInputStream(jar);
	}
}
