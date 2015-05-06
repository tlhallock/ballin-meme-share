package org.cnv.shr.stng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Properties;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;

public class Settings implements SettingListener
{
	public static final String encoding = "UTF8";
	public static final String checksumAlgorithm = "SHA1";
	public static final String encryptionAlgorithm = "RSA";
	public static final String VERSION = "0.1";
	
	private static final String SQL_DIR  = "sql";

	private File settingsFile;
	private String localAddress;
	private LinkedList<Setting> settings = new LinkedList<>();
	private boolean reading;
	
	public StringSetting     machineName              = new StringSetting    ("machineName         ".trim(), Misc.getRandomName()                                                         , false, true, "The name to display for this machine.                                       ".trim()); { settings.add(machineName         );  }
	public StringSetting     machineIdentifier        = new StringSetting    ("machineIdentifier   ".trim(), Misc.getRandomString(50)                                                     , false, true, "A unique identifer for this machine.                                        ".trim()); { settings.add(machineIdentifier   );  }
	public IntSetting        servePortBegin           = new IntSetting       ("servePortBegin      ".trim(), 8990                                          , 1, Integer.MAX_VALUE         , false, true, "Smallest port to use for listening for other machines.                      ".trim()); { settings.add(servePortBegin      );  }
	public IntSetting        maxDownloads             = new IntSetting       ("maxDownloads        ".trim(), 20                                            , 1, Integer.MAX_VALUE         , false, true, "Maximum number of concurrent downloads.                                     ".trim()); { settings.add(maxDownloads        );  }
	public IntSetting        maxServes                = new IntSetting       ("maxServes           ".trim(), 20                                            , 1, Integer.MAX_VALUE         , false, true, "Number of ports to listen on.                                               ".trim()); { settings.add(maxServes           );  }
	public IntSetting        minNaunce                = new IntSetting       ("minNaunce           ".trim(), 256                                           , 1, Integer.MAX_VALUE         , false, true, "Minimum size of a Naunce for authentication.                                ".trim()); { settings.add(minNaunce           );  }
	public IntSetting        maxStringSize            = new IntSetting       ("maxStringSize       ".trim(), 4096                                          , 1, Integer.MAX_VALUE         , false, true, "                                                                            ".trim()); { settings.add(maxStringSize       );  }
	public IntSetting        numThreads               = new IntSetting       ("numThreads          ".trim(), 20                                            , 1, Integer.MAX_VALUE         , false, true, "                                                                            ".trim()); { settings.add(numThreads          );  }
	public LongSetting       maxDirectorySize         = new LongSetting      ("maxDirectorySize    ".trim(), -1                                                                           , false, true, "Maximum size in bytes to let the downloads directory be. (-1 for unlimited).".trim()); { settings.add(maxDirectorySize    );  }
	public LongSetting       monitorRepeat            = new LongSetting      ("monitorRepeat       ".trim(), 10 * 60 * 1000                                                               , false, true, "Frequency in ms to update local files (-1 for never).                       ".trim()); { settings.add(monitorRepeat       );  }
	public LongSetting       checksumWait             = new LongSetting      ("checksumWait        ".trim(), 1 * 200                                                                      , false, true, "Time to pause between checksumming large files/                             ".trim()); { settings.add(checksumWait        );  }
	public LongSetting       maxImmediateChecksum     = new LongSetting      ("maxImmediateChecksum".trim(), 1 * 1024 * 1024                                                              , false, true, "Maximum file size in bytes to checksum immediately.                         ".trim()); { settings.add(maxImmediateChecksum);  }
	public LongSetting       maxChunkSize             = new LongSetting      ("maxChunkSize        ".trim(), 1024                                                                         , false, true, "Maximum size of any chunk to send accross the network.                      ".trim()); { settings.add(maxChunkSize        );  }
	public BooleanSetting    logToFile                = new BooleanSetting   ("logToFile           ".trim(), true                                                                         , false, true, "Should log messages be written to file.                                     ".trim()); { settings.add(logToFile           );  }
	public DirectorySetting  downloadsDirectory       = new DirectorySetting ("downloadsDirectory  ".trim(), new File("."                                  + File.separator + "downloads"), false, true, "Directory to put downloaded files.                                          ".trim()); { settings.add(downloadsDirectory  );  }
	public DirectorySetting  applicationDirectory     = new DirectorySetting ("applicationDirectory".trim(), new File("."                                  + File.separator + "app"      ), false, true, "Directory to put application files.                                         ".trim()); { settings.add(applicationDirectory);  }
	public DirectorySetting  stagingDirectory         = new DirectorySetting ("stagingDirectory    ".trim(), new File("."                                  + File.separator + "tempD"    ), false, true, "Directory to put files currently being served.                              ".trim()); { settings.add(stagingDirectory    );  }
	public DirectorySetting  servingDirectory         = new DirectorySetting ("servingDirectory    ".trim(), new File("."                                  + File.separator + "tempS"    ), false, true, "Directory to put files currently being downloaded.                          ".trim()); { settings.add(servingDirectory    );  }
	public FileSetting       keysFile                 = new FileSetting      ("keysFile            ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "keys.json"), false, true, "File to put public/private key information.                                 ".trim()); { settings.add(keysFile            );  }
	public FileSetting       logFile                  = new FileSetting      ("logFile             ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "log.txt"  ), false, true, "File to put log messages if logging to file.                                ".trim()); { settings.add(logFile             );  }
	public FileSetting       dbFile                   = new FileSetting      ("dbFile              ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "files.db" ), false, true, "File to put database.                                                       ".trim()); { settings.add(dbFile              );  }
	
	public Settings(File settingsFile) throws UnknownHostException
	{
		this.settingsFile = settingsFile;
		localAddress = InetAddress.getLocalHost().getHostAddress();
		Services.logger.logStream.println("Local host is " + localAddress);
		for (Setting setting : settings)
		{
			setting.addListener(this);
		}
	}

	public synchronized void write() throws FileNotFoundException, IOException
	{
		Properties properties = new Properties();
		for (Setting setting : settings)
		{
			setting.save(properties);
		}

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

	private synchronized void read(Properties properties) throws FileNotFoundException, IOException
	{
		try
		{
			reading = true;
			for (Setting setting : settings)
			{
				setting.read(properties);
			}
		}
		finally
		{
			reading = false;
		}
	}
	
	public void setToDefaults() throws FileNotFoundException, IOException
	{
		try
		{
			reading = true;
			for (Setting setting : settings)
			{
				setting.resetToDefaults();
			}
		}
		finally
		{
			reading = false;
		}
		write();
	}

	public String getSqlDir()
	{
		return /*applicationDirectory +*/ SQL_DIR;
	}

	public String getLocalIp()
	{
		return localAddress;
	}

	@Override
	public void settingChanged()
	{
		if (reading)
		{
			return;
		}
		try
		{
			write();
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to save settings.");
			e.printStackTrace(Services.logger.logStream);
		}
	}

    public Setting[] getUserSettings() {
        return settings.toArray(new Setting[0]);
    }
}
