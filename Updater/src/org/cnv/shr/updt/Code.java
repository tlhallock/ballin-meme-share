package org.cnv.shr.updt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.zip.ZipException;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.ProcessInfo;

public class Code
{
	private long timeStamp;
	private Path jar = Updater.getUpdatesDirectory().resolve("ConvenienceShare.jar");
	private String version;
	
	public Code() throws ZipException, IOException
	{
		checkTime();
		// Add watch service?
	}
	
	public void checkTime()
	{
		long fsTime;
		try
		{
			fsTime = Files.getLastModifiedTime(jar).toMillis();
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get fs time.", e1);
			return;
		}

		if (timeStamp > fsTime)
		{
			return;
		}
		
		version = ProcessInfo.getJarVersion(jar);
		if (version != null)
		{
			timeStamp = fsTime;
		}
	}
	
	public String getVersion()
	{
		checkTime();
		return version;
	}

	public InputStream getStream() throws IOException
	{
		checkTime();
		return Files.newInputStream(jar);
	}
}
