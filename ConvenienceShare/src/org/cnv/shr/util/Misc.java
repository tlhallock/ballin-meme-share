package org.cnv.shr.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
	
	public static byte[] createNaunce()
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
	public static String getIp(byte[] address)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(address[0]);
		for (int i=1;i<address.length;i++)
		{
			sb.append(".").append(address[i]);
		}
		return sb.toString();
	}
	
	public static void copy(InputStream input, OutputStream output) throws IOException
	{
		int readByte;
		while ((readByte = input.read()) >= 0)
		{
			output.write(readByte);
		}
	}
	
	public static void rm(Path path) throws IOException
	{
		if (Files.isSymbolicLink(path) || Files.isRegularFile(path))
		{
			System.out.println("Deleting " + path.toString());
			Files.delete(path);
		}
		else if (Files.isDirectory(path))
		{
			DirectoryStream<Path> stream = Files.newDirectoryStream(path);
			for (Path child : stream)
			{
				rm(child);
			}
			Files.delete(path);
		}
	}

	public static String getJarPath()
	{
		String path = Services.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		try
		{
			return URLDecoder.decode(path, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			Services.logger.print(e);
			return path;
		}
	}
	public static String getJavaPath()
	{
		return "/usr/bin/java";
	}
	public static String getJarName()
	{
		return "org.cnv.shr.dmn.mn.MainTest";
	}
	public static String getClassPath()
	{
		return "../libs/h2-1.4.187.jar:../libs/org.json-20120521.jar:../lib/CoDec-build17-jdk13.jar:../lib/FlexiProvider-1.7p7.signed.jar:.";
	}
}
