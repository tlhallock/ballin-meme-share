package org.cnv.shr.dmn;

import java.util.List;

public class Settings
{
	String settingsFile;

	String downloadsDirectory;
	String applicationCache;
	List<String> allowedUsers;
	long maxDirectorySize;
	int defaultPort = 8989;
	int maxDownloads = 10;
	int maxServes = 10;
	int maxStringSize = 4096;
	int numThreads = 10;

	/** This should be unique **/
	String userId;

	private Settings()
	{
	}

	public int getDefaultPort()
	{
		return defaultPort;
	}

	public int getMaxStringSize()
	{
		return maxStringSize;
	}

	public String getEncoding()
	{
		return "UTF8";
	}

	public int getNumThreads()
	{
		return numThreads;
	}

	private static Settings settings = new Settings();

	public static Settings getInstance()
	{
		return settings;
	}

}
