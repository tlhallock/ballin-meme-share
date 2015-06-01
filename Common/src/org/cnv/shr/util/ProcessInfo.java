package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


public class ProcessInfo
{

	public static Path getJarPath(Class c)
	{
		String path = c.getProtectionDomain().getCodeSource().getLocation().getPath();

		try
		{
			return Paths.get(URLDecoder.decode(path, "UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
			e.printStackTrace();
			return Paths.get(path);
		}
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
	public static String getClassPath()
	{
		return "../lib/h2-1.4.187.jar:../lib/h2-1.3.175.jar:../lib/CoDec-build17-jdk13.jar:../lib/FlexiProvider-1.7p7.signed.jar:.";
	}
	
	public static String getJarVersion(Path jar)
	{
		try (ZipFile zipFile = new ZipFile(jar.toFile());
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipFile.getEntry("res/version.txt"))));)
		{
			String version = Misc.readAll(inputStream);
			LogWrapper.getLogger().info("Found version " + version);
			return version;
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
