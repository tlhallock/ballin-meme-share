package org.cnv.shr.updt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class Code
{
	private long timeStamp;
	private File jar = new File (Updater.getUpdatesDirectory() + "ConvenienceShare.jar");
	private String version;
	
	public Code() throws ZipException, IOException
	{
		checkTime();
	}
	
	public void checkTime()
	{
		if (timeStamp > jar.lastModified())
		{
			return;
		}
		long newTimeStamp = jar.lastModified();
		
		try (ZipFile zipFile = new ZipFile(jar);
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipFile.getEntry("res/version.txt"))));)
		{
			version = Misc.readAll(inputStream);
		}
		catch (ZipException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Bad jar file.", e);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read jar.", e);
		}
		
		timeStamp = newTimeStamp;
	}
	
	
	
	public String getVersion()
	{
		checkTime();
		return version;
	}

	public InputStream getStream() throws FileNotFoundException
	{
		checkTime();
		return new FileInputStream(jar);
	}
}
