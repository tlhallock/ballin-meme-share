package org.cnv.shr.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.cnv.shr.stng.Settings;

public class Misc
{
	public static final String INITIALIZED_STRING = "Succesfully Initialized.";
	private static final Random random = new Random();

	public static void ensureDirectory(Path path, boolean file)
	{
		ensureDirectory(path.toFile(), file);
	}
	public static void ensureDirectory(String path, boolean file)
	{
		ensureDirectory(new File(path), file);
	}
	public static void ensureDirectory(File f, boolean file)
	{
		if (file)
		{
			f = f.getParentFile();
			if (f == null)
			{
				f = new File(".");
			}
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
	
	public static byte[] createNaunce(int length)
	{
		return getBytes(length); 
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

	// This should be in IOUtils, and I think I have written it once already...
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

	
	public static final int ICON_SIZE = 22;
	public static Image getIcon() throws IOException
	{
		BufferedImage read = ImageIO.read(ClassLoader.getSystemResourceAsStream(Settings.RES_DIR + "icon.png"));
		Image scaledInstance = read.getScaledInstance(ICON_SIZE, ICON_SIZE, BufferedImage.SCALE_SMOOTH);
		return scaledInstance;
	}
	
	public static String readFile(String resourceName)
	{
		InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(resourceName);
		Objects.requireNonNull(systemResourceAsStream, "Jar is missing file " + resourceName);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(systemResourceAsStream)))
		{
			return readAll(reader);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to read resource " + resourceName + ".", e);
			return null;
		}
	}
	public static String readAll(BufferedReader reader) throws IOException
	{
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null)
		{
			builder.append(line); // careful what happens to the new lines...
		}
		return builder.toString();
	}
	public static void writeBytes(byte[] bytes, OutputStream output) throws IOException
	{
		output.write((byte) ((bytes.length >> 24) & 0xff));
		output.write((byte) ((bytes.length >> 16) & 0xff));
		output.write((byte) ((bytes.length >>  8) & 0xff));
		output.write((byte) ((bytes.length >>  0) & 0xff));
		output.write(bytes);
	}
	public static byte[] readBytes(InputStream input) throws IOException
	{
		int length = 0;
		length |= (input.read() & 0xff) << 24;
		length |= (input.read() & 0xff) << 16;
		length |= (input.read() & 0xff) <<  8;
		length |= (input.read() & 0xff) <<  0;
		
		byte[] returnValue = new byte[length];
		if (length == 0)
		{
			return returnValue;
		}
		int offset = 0;
		while ((offset += input.read(returnValue, offset, length - offset)) < length)
			;
		return returnValue;
	}
	
	private static OperatingSystem system = OperatingSystem.getOperatingSystem();
	public static String sanitizePath(String path)
	{
		if (!system.equals(OperatingSystem.Windows))
		{
			return path;
		}
		
		path = path.replace('\\', '/');
//		int ndx = path.indexOf(':');
//		if (ndx >= 0)
//		{
//			path = path.substring(ndx+1, path.length());
//		}
		
		return path;
	}
	
	public static String deSanitize(String path)
	{
		if (!system.equals(OperatingSystem.Windows))
		{
			return path;
		}
		
		path = path.replace('/', '\\');
		
		return path;
	}
	
	
	public enum OperatingSystem
	{
		Linux  (new String[] {"Linux",           }),
		Windows(new String[] {"Windows"     ,    }),
		Apple  (new String[] {"Fill this in",    }),
		
		;
		
		String[] patterns;
		
		OperatingSystem(String[] p)
		{
			this.patterns = p;
		}
		
		public boolean is(String osName)
		{
			for (String pattern : patterns)
			{
				if (osName.contains(pattern))
				{
					return true;
				}
			}
			return false;
		}
		
		public static OperatingSystem getOperatingSystem()
		{
			String osName = System.getProperty("os.name");
			for (OperatingSystem os : values())
			{
				if (os.is(osName))
				{
					return os;
				}
			}
			
			LogWrapper.getLogger().severe("Unkown operating system type: " + osName);
			System.exit(-1);
			return null;
		}
	}
	
	private static final List<String> getList(String... args)
	{
		LinkedList<String> returnValue = new LinkedList<>();
		for (String str : args)
		{
			returnValue.add(str);
		}
		return returnValue;
	}
	
	public static void nativeOpen(Path f)
	{
		LinkedList<String> returnValue = new LinkedList<>();
		switch (system)
		{
		case Windows:
			returnValue.add("explorer.exe");
			if (Files.isRegularFile(f))
			{
				// With this argument the file itself is actually opened...
				returnValue.add(f.getParent().toString());
				returnValue.add("/select," + f.toString());
			}
			else
			{
				returnValue.add(f.toString());
			}
			break;
		case Linux:
			throw new RuntimeException("Implement me!");
		case Apple:
			throw new RuntimeException("Implement me!");
		}
		
		
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(returnValue);
		try
		{
			builder.start();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to start open process", e);
		}
	}
	
	
//	public static void listRemotes(PrintStream ps)
//	{
//		try (DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();)
//		{
//			while (listRemoteMachines.hasNext())
//			{
//				Machine next = listRemoteMachines.next();
//				String ip = next.getIp();
//				int port = next.getPort();
//				for (int i = 0; i < next.getNumberOfPorts(); i++)
//				{
//					ps.print(ip + ":" + (port + i) + " ");
//				}
//				ps.println();
//			}
//		}
//	}
//	
//	public static void readRemotes(URL url) throws IOException
//	{
//		readRemotes(new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())));
//	}
//	
//	public static void readRemotes(BufferedReader reader) throws IOException
//	{
//		String line;
//		AddMachineParams params = new AddMachineParams(true);
//		while ((line = reader.readLine()) != null)
//		{
//			try (Scanner scanner = new Scanner(line);)
//			{
//				while (scanner.hasNext())
//				{
//					String url = scanner.next();
//					UserActions.addMachine(url, params);
//					break;
//				}
//			}
//		}
//	}
}