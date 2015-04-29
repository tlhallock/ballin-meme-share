package org.cnv.shr.util;

import java.io.File;

public class Misc
{
	public static void ensureDirectory(String path, boolean file)
	{
		ensureDirectory(new File(path), file);
	}
	public static void ensureDirectory(File f, boolean file)
	{
		if (file)
		{
			f = f.getParentFile();
		}
		f.mkdirs();
	}
	
	public static String format(byte[] bytes)
	{
		StringBuilder builder = new StringBuilder();
		
		for (byte b : bytes)
		{
			builder.append(String.format("%02X", b & 0xff));
		}
		
		return builder.toString();
	}
	
	public static byte[] format(String str)
	{
		byte[] returnValue = new byte[str.length() / 2];
		
		for (int i=0;i<returnValue.length;i++)
		{
			returnValue[i] = (byte) Integer.parseInt(str.charAt(2*i) + "" + str.charAt(2*i+1), 16);
		}

		return returnValue;
	}
}
