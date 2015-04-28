package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;

public class Settings
{
	public static final String encoding = "UTF8";
	public static final String checksumAlgorithm = "SHA1";
	
	public String downloadsDirectory;
	public String applicationDirectory;
	public String stagingDirectory;
	
	public int defaultPort;
	public int servePortBegin;
	public int servePortEnd;
	
	public int maxDownloads;
	public int maxServes;
	
	public int maxStringSize;
	public int numThreads;
	public long maxDirectorySize;

	public PublicKey publicKey;
	public PrivateKey privateKey;
	
	public String logLocation;
	public long monitorRepeat;

	public long checksumWait;

	private static int getInt(Properties p, String key, String defaultValue)
	{
		return (int) getLong(p, key, defaultValue);
	}
	private static long getLong(Properties p, String key, String defaultValue)
	{
		try
		{
			return Long.parseLong(p.getProperty(key, defaultValue));
		}
		catch (Exception ex)
		{
			return Long.parseLong(defaultValue);
		}
	}
	
	public synchronized void write(File f) throws FileNotFoundException, IOException
	{
		Properties properties = new Properties();

		properties.setProperty("dirs.downloads",   String.valueOf(downloadsDirectory));
		properties.setProperty("dirs.application", String.valueOf(applicationDirectory));
		properties.setProperty("dirs.staging",     String.valueOf(stagingDirectory));
		properties.setProperty("files.log",        String.valueOf(logLocation));
		properties.setProperty("port.server",      String.valueOf(defaultPort));
		properties.setProperty("port.begin",       String.valueOf(servePortBegin));
		properties.setProperty("port.end",         String.valueOf(servePortEnd));
		properties.setProperty("max.threads",      String.valueOf(numThreads));
		properties.setProperty("max.down",         String.valueOf(maxDownloads));
		properties.setProperty("max.up",           String.valueOf(maxServes));
		properties.setProperty("max.str",          String.valueOf(maxStringSize));
		properties.setProperty("max.port",         String.valueOf(maxDirectorySize));
		properties.setProperty("monitor.repeat",   String.valueOf(monitorRepeat));

		try (FileOutputStream outputStream = new FileOutputStream(f))
		{
			properties.store(outputStream, null);
		}
	}
	
	public synchronized void read(File f) throws FileNotFoundException, IOException
	{
		Properties properties = new Properties();
		try (FileInputStream inStream = new FileInputStream(f))
		{
			properties.load(inStream);
		}
		read(properties);
	}
	
	public void setToDefaults() throws FileNotFoundException, IOException
	{
		read(new Properties());
	}

	private synchronized void read(Properties properties) throws FileNotFoundException, IOException
	{
		downloadsDirectory   = properties.getProperty("dirs.downloads",       "./downloads");
		applicationDirectory = properties.getProperty("dirs.application",           "./app");
		stagingDirectory     = properties.getProperty("dirs.staging",              "./temp");
		logLocation          = properties.getProperty("files.log",              "./log.txt");
		defaultPort          = getInt(properties,     "port.server",                 "8989");
		servePortBegin       = getInt(properties,     "port.begin",                  "8990");
		servePortEnd         = getInt(properties,     "port.end",                    "9000");
		numThreads           = getInt(properties,     "max.threads",                   "10");
		maxDownloads         = getInt(properties,     "max.down",                      "10");
		maxServes            = getInt(properties,     "max.up",                        "10");
		maxStringSize        = getInt(properties,     "max.str",                     "4096");
		maxDirectorySize     = getInt(properties,     "max.port",                      "-1");
		monitorRepeat        = getInt(properties,     "monitor.repeat",             "50000");
	}
}
