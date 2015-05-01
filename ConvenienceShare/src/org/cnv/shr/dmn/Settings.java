package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.cnv.shr.util.Misc;

public class Settings
{
	public static final String encoding = "UTF8";
	public static final String checksumAlgorithm = "SHA1";
	public static String encryptionAlgorithm = "RSA";
	
	private static final String KEYS_FILE = File.separatorChar + "keys.json";
	private static final String LOG_FILE = File.separatorChar + "log.txt";
	private static final String SQL_DIR = "sql";
	private static final String DB_FILE = File.separatorChar + "files.db";

	public static boolean logToFile = false;
	
	public String machineName = "foobar";
	public String machineIdentifier;
	
	public String downloadsDirectory;
	public String applicationDirectory;
	public String stagingDirectory;
	
	public int defaultPort;
	public int servePortBegin;
	public int servePortEnd;
	
	public int maxDownloads;
	public int maxServes;
	public int minNaunce;
	
	public int maxStringSize;
	public int numThreads;
	public long maxDirectorySize;
	
	public long monitorRepeat;

	public long checksumWait;
	public long maxImmediateChecksum = 1 * 1024 * 1024;
	
	private File settingsFile;
	private String localAddress;
	
	Settings(File settingsFile) throws UnknownHostException
	{
		this.settingsFile = settingsFile;
		localAddress = InetAddress.getLocalHost().getHostAddress();
		Services.logger.logStream.println("Local host is " + localAddress);
	}

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
	
	public synchronized void write() throws FileNotFoundException, IOException
	{
		Properties properties = new Properties();

		properties.setProperty("dirs.downloads",   String.valueOf(downloadsDirectory  ));
		properties.setProperty("dirs.application", String.valueOf(applicationDirectory));
		properties.setProperty("dirs.staging",     String.valueOf(stagingDirectory    ));
		properties.setProperty("machinename",      String.valueOf(machineName         ));
		properties.setProperty("port.server",      String.valueOf(defaultPort         ));
		properties.setProperty("port.begin",       String.valueOf(servePortBegin      ));
		properties.setProperty("port.end",         String.valueOf(servePortEnd        ));
		properties.setProperty("max.threads",      String.valueOf(numThreads          ));
		properties.setProperty("max.down",         String.valueOf(maxDownloads        ));
		properties.setProperty("max.up",           String.valueOf(maxServes           ));
		properties.setProperty("max.str",          String.valueOf(maxStringSize       ));
		properties.setProperty("max.port",         String.valueOf(maxDirectorySize    ));
		properties.setProperty("min.naunce",       String.valueOf(minNaunce           ));
		properties.setProperty("monitor.repeat",   String.valueOf(monitorRepeat       ));
		properties.setProperty("identifier",       String.valueOf(machineIdentifier   ));


		Misc.ensureDirectory(settingsFile, true);
		try (FileOutputStream outputStream = new FileOutputStream(settingsFile))
		{
			properties.store(outputStream, null);
		}
	}
	
	public synchronized void read() throws FileNotFoundException, IOException
	{
		Properties properties = new Properties();
		try (FileInputStream inStream = new FileInputStream(settingsFile))
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
		downloadsDirectory   = properties.getProperty( "dirs.downloads",             "downloads");
		applicationDirectory = properties.getProperty( "dirs.application",                 "app");
		stagingDirectory     = properties.getProperty( "dirs.staging",                    "temp");
		machineName          = properties.getProperty( "machinename",                   "foobar");
		machineIdentifier    = properties.getProperty( "identifier",    Misc.getRandomString(50));
		defaultPort          = getInt (properties,     "port.server",                     "8989");
		servePortBegin       = getInt (properties,     "port.begin",                      "8990");
		servePortEnd         = getInt (properties,     "port.end",                        "9000");
		numThreads           = getInt (properties,     "max.threads",                       "10");
		maxDownloads         = getInt (properties,     "max.down",                          "10");
		maxServes            = getInt (properties,     "max.up",                            "10");
		maxStringSize        = getInt (properties,     "max.str",                         "4096");
		minNaunce            = getInt (properties,     "min.naunce",                       "128");
		maxDirectorySize     = getLong(properties,     "max.port",                          "-1");
		monitorRepeat        = getLong(properties,     "monitor.repeat",                 "50000");
	}
	

	/**
	public File getLocalsFile()
	{
		return new File(applicationDirectory + LOCALS_FILE);
	}
	public File getRemotesFile()
	{
		return new File(applicationDirectory + REMOTES_FILE);
	}
	**/
	public File getKeysFile()
	{
		return new File(applicationDirectory + KEYS_FILE);
	}
	public File getLogFile()
	{
		return new File(applicationDirectory + LOG_FILE);
	}
	public String getSqlDir()
	{
		return /*applicationDirectory +*/ SQL_DIR;
	}
	public String getDbFile()
	{
		return applicationDirectory + DB_FILE;
	}

	public String getLocalIp()
	{
		return localAddress;
	}
}
