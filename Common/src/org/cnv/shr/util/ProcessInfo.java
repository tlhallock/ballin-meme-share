package org.cnv.shr.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class ProcessInfo
{

	public static String getJarPath(Class c)
	{
		String path = c.getProtectionDomain().getCodeSource().getLocation().getPath();
		try
		{
			return URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
			e.printStackTrace();
			return path;
		}
	}
	public static String getJavaPath()
	{
		return "/usr/bin/java";
	}
	public static String getClassPath()
	{
		return "../lib/h2-1.4.187.jar:../lib/h2-1.3.175.jar:../lib/CoDec-build17-jdk13.jar:../lib/FlexiProvider-1.7p7.signed.jar:.";
	}
}
