
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


public class ProcessInfo
{

	public static Path getJarPath(Class c)
	{
		String path = c.getProtectionDomain().getCodeSource().getLocation().getPath();
		switch (Misc.getOperatingSystem())
		{
		case Mac:   return Paths.get(path);
		case Linux:   return Paths.get(path);
		case Windows: 
			if (path.startsWith("/"))
			{
				path = path.substring(1);
			}
			return Paths.get(path);
		default:
			break;
			
		}
		
		Paths.get("C:/Users/thallock/Documents/Source/ballin-meme-share/ConvenienceShare/bin/");

//		try
//		{
			return Paths.get(path);
					
					
					
//					URLDecoder.decode(path, "UTF-8"));
//		}
//		catch (UnsupportedEncodingException e)
//		{
////			LogWrapper.getLogger().log(Level.INFO, , e);
//			e.printStackTrace();
//			return Paths.get(path);
//		}
	}
	
	private static String getJarName()
	{
		return "ConvenienceShare.jar";
	}
	
	public static Path getJarFile(Class c)
	{
		return getJarPath(c); //.resolve(getJarName());
	}
	
	
	public static String getJavaPath()
	{
		return "/usr/bin/java";
	}
	public static String getTestClassPath()
	{
		return "../lib/h2-1.4.187.jar:../lib/h2-1.3.175.jar:../lib/CoDec-build17-jdk13.jar:../lib/FlexiProvider-1.7p7.signed.jar:../../Common/bin:.";
	}
	
	public static String getJarVersion(Path jar)
	{
		try (ZipFile zipFile = new ZipFile(jar.toFile());
				InputStream inputStream = zipFile.getInputStream(zipFile.getEntry("META-INF/MANIFEST.MF"));)
		{
			return new Manifest(inputStream).getMainAttributes().getValue("Implementation-Version");
		}
		catch (ZipException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Bad jar file.", e);
			return null;
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read jar.", e);
			return null;
		}
	}
}
