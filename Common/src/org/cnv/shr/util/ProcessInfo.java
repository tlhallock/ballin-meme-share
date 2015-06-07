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
