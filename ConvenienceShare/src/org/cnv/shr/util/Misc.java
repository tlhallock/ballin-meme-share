package org.cnv.shr.util;

import java.io.File;
import java.util.Random;

import org.cnv.shr.dmn.Services;

public class Misc
{
	private static final Random random = new Random();
	
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

		for (int i = 0; i < returnValue.length; i++)
		{
			returnValue[i] = (byte) Integer.parseInt(str.charAt(2*i) + "" + str.charAt(2*i+1), 16);
		}

		return returnValue;
	}
	
	private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	public static String getRandomString(int size)
	{
		StringBuilder builder = new StringBuilder(size);
		for (int i = 0; i < size; i++)
		{
			builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
		}
		return builder.toString();
	}
	
	public static byte[] getNaunce()
	{
		return getBytes(Services.settings.minNaunce.get());
	}
	public static byte[] getBytes(int length)
	{
		byte[] returnValue = new byte[length];
		random.nextBytes(returnValue);
		return returnValue;
	}
	
	public static String formatNumberOfFiles(long numFiles)
	{
		String number = String.valueOf(numFiles);
		if (number.length() <= 3)
		{
			return number;
		}
		StringBuilder builder = new StringBuilder();
		
		int offset = number.length() % 3;
		if (offset == 0) offset = 3;
		builder.append(number.substring(0, offset));
		
		while (offset + 3 <= number.length())
		{
			offset += 3;
			builder.append(',').append(number.substring(offset-3, offset));
		}
		
		return builder.toString();
	}
	
	public static String formatDiskUsage(long bytes)
	{
		if (bytes < 1024)
		{
			return bytes + " b";
		}
		
		double totalFileSize = bytes;
		totalFileSize /= 1024;
		if (totalFileSize < 1024.0)
		{
			return String.format("%.2f Kb", totalFileSize);
		}

		totalFileSize /= 1024;
		if (totalFileSize < 1024.0)
		{
			return String.format("%.2f Mb", totalFileSize);
		}

		totalFileSize /= 1024;
		if (totalFileSize < 1024.0)
		{
			return String.format("%.2f Gb", totalFileSize);
		}

		totalFileSize /= 1024;
		return String.format("%.2f Tb", totalFileSize);
	}
	public static String getRandomName()
	{
		return getRandomString(10);
	}
}
