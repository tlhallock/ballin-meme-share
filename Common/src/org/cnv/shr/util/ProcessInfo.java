
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



package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class ProcessInfo
{
	public static Path getJarPath(Class<?> c)
	{
		String path = c.getProtectionDomain().getCodeSource().getLocation().getPath();
		switch (Misc.getOperatingSystem())
		{
		case Mac:   break;
		case Linux: break;
		case Windows: 
			if (path.startsWith("/"))
			{
				path = path.substring(1);
			}
			break;
		default:
			break;
		}
		
		LogWrapper.getLogger().info("The found jar path is \"" + path + "\"");
		
		if (path.endsWith(".jar"))
		{
			return Paths.get(path).getParent();
		}
		
		return Paths.get(path);
	}
	
	public static String getTestClassPath()
	{
		// Needs to be updated...
		return "../lib/h2-1.4.187.jar:../lib/h2-1.3.175.jar:../lib/CoDec-build17-jdk13.jar:../lib/FlexiProvider-1.7p7.signed.jar:../../Common/bin:.";
	}
	
	public static String getJarVersionFromUpdates(Path file, String jar)
	{
		try (ZipFile zipFile = new ZipFile(file.toFile());
				InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(jar));
				ZipInputStream subStream = new ZipInputStream(inputStream))
		{
			if (!findZipEntry(subStream, "META-INF/MANIFEST.MF"))
			{
				LogWrapper.getLogger().info("Bad jar file: can't find manifest");
				return null;
			}
			return new Manifest(subStream).getMainAttributes().getValue("Implementation-Version");
		}
		catch (ZipException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Bad jar file: no convenience share jar.", e);
			return null;
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read jar.", e);
			return null;
		}
	}
	
	public static boolean findZipEntry(ZipInputStream input, String name) throws IOException
	{
		while (true)
		{
			ZipEntry nextEntry = input.getNextEntry();
			if (nextEntry == null)
			{
				return false;
			}
			if (nextEntry.getName().equals(name))
			{
				return true;
			}
		}
	}

	private static boolean isBinaryFile(Path exe)
	{
		return exe != null && Files.exists(exe) && Files.isRegularFile(exe) && Files.isExecutable(exe);
	}
	private static Path javaBinary = null;
	public synchronized static String getJavaBinary()
	{
		if (isBinaryFile(javaBinary))
		{
			return javaBinary.toString();
		}
		
		String javaHome = System.getProperty("java.home");
		if (javaHome == null)
		{
			return "java"; // This is as good as we can do
		}
		Path homePath = Paths.get(javaHome);
		if (!Files.exists(homePath))
		{
			return "java";
		}
		Path binDir = homePath.resolve("bin");
		if (!Files.exists(binDir))
		{
			return "java";
		}
		for (String exeName : new String[] {
				"java",
				"java.exe",
				"javaw",
				"javaw.exe",
		})
		{
			Path exe = binDir.resolve(exeName);
			if (isBinaryFile(exe))
			{
				return (javaBinary = exe).toString();
			}
		}
		return "java";
	}
}
