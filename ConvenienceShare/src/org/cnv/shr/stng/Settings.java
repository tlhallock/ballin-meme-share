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
	
	public static final String SQL_DIR  = "sql/";
	public static final String IMG_DIR  = "imgs/";

	private File settingsFile;
	private String localAddress;
	private LinkedList<Setting> settings = new LinkedList<>();
	private boolean reading;
	
	public StringSetting     machineName              = new StringSetting    ("machineName         ".trim(), Misc.getRandomName()                                                         , false, true, "The name to display for this machine.                                           ".trim()); { settings.add(machineName         );  }
	public StringSetting     machineIdentifier        = new StringSetting    ("machineIdentifier   ".trim(), Misc.getRandomString(50)                                                     , false, true, "A unique identifer for this machine.                                            ".trim()); { settings.add(machineIdentifier   );  }
	public IntSetting        servePortBeginI          = new IntSetting       ("servePortBeginI     ".trim(), 8990                                       ,    1, Integer.MAX_VALUE         , false, true, "Smallest port to use for listening for other machines. (Router external port.)  ".trim()); { settings.add(servePortBeginI     );  }
	public IntSetting        servePortBeginE          = new IntSetting       ("servePortBeginE     ".trim(), 8990                                       ,    1, Integer.MAX_VALUE         , false, true, "Smallest port to use for listening for other machines. (Router internal port.)  ".trim()); { settings.add(servePortBeginE     );  }
	public IntSetting        maxDownloads             = new IntSetting       ("maxDownloads        ".trim(), 20                                         ,    1, Integer.MAX_VALUE         , false, true, "Maximum number of concurrent downloads.                                         ".trim()); { settings.add(maxDownloads        );  }
	public IntSetting        maxServes                = new IntSetting       ("maxServes           ".trim(), 20                                         ,    1, Integer.MAX_VALUE         , false, true, "Number of ports to listen on.                                                   ".trim()); { settings.add(maxServes           );  }
	public IntSetting        minNaunce                = new IntSetting       ("minNaunce           ".trim(), 117                                        ,    1, Integer.MAX_VALUE         , false, true, "Minimum size of a Naunce for authentication.                                    ".trim()); { settings.add(minNaunce           );  }
	public IntSetting        maxStringSize            = new IntSetting       ("maxStringSize       ".trim(), 4096                                       ,    1, Integer.MAX_VALUE         , false, true, "                                                                                ".trim()); { settings.add(maxStringSize       );  }
	public IntSetting        numThreads               = new IntSetting       ("numThreads          ".trim(), 20                                         ,    1, Integer.MAX_VALUE         , false, true, "                                                                                ".trim()); { settings.add(numThreads          );  }
	public IntSetting        keySize                  = new IntSetting       ("keySize             ".trim(), 1024                                       , 1024,              4096         , false, true, "                                                                                ".trim()); { settings.add(keySize             );  }
	public IntSetting        appLocX                  = new IntSetting       ("appLocX             ".trim(), 0                                                                            , false, true, "Place to put windows on screen x.                                               ".trim()); { settings.add(appLocX             );  }
	public IntSetting        appLocY                  = new IntSetting       ("appLocY             ".trim(), 0                                                                            , false, true, "Place to put windows on screen y.                                               ".trim()); { settings.add(appLocY             );  }
	public LongSetting       maxDirectorySize         = new LongSetting      ("maxDirectorySize    ".trim(), -1                                                                           , false, true, "Maximum size in bytes to let the downloads directory be. (-1 for unlimited).    ".trim()); { settings.add(maxDirectorySize    );  }
	public LongSetting       monitorRepeat            = new LongSetting      ("monitorRepeat       ".trim(), 10 * 60 * 1000                                                               , false, true, "Frequency in ms to update local files (-1 for never).                           ".trim()); { settings.add(monitorRepeat       );  }
	public LongSetting       checksumWait             = new LongSetting      ("checksumWait        ".trim(), 1 * 200                                                                      , false, true, "Time to pause between checksumming large files/                                 ".trim()); { settings.add(checksumWait        );  }
	public LongSetting       maxImmediateChecksum     = new LongSetting      ("maxImmediateChecksum".trim(), 1 * 1024 * 1024                                                              , false, true, "Maximum file size in bytes to checksum immediately.                             ".trim()); { settings.add(maxImmediateChecksum);  }
	public LongSetting       maxChunkSize             = new LongSetting      ("maxChunkSize        ".trim(), 1024                                                                         , false, true, "Maximum size of any chunk to send accross the network.                          ".trim()); { settings.add(maxChunkSize        );  }
	public LongSetting       logLength                = new LongSetting      ("logLength            ".trim(), 20 * 1024 * 1024                                                            , false, true, "File to put log messages if logging to file.                                    ".trim()); { settings.add(logLength           );  }
	public BooleanSetting    logToFile                = new BooleanSetting   ("logToFile           ".trim(), true                                                                         , false, true, "Should log messages be written to file.                                         ".trim()); { settings.add(logToFile           );  }
	public BooleanSetting    acceptNewKeys            = new BooleanSetting   ("acceptNewKeys       ".trim(), false                                                                        , false, true, "True if all new keys should be accepted.                                        ".trim()); { settings.add(acceptNewKeys       );  }
	public BooleanSetting    shareWithEveryone        = new BooleanSetting   ("shareWithEveryone   ".trim(), false                                                                        , false, true, "True you would like to let anyone download from this machine.                   ".trim()); { settings.add(shareWithEveryone   );  }
	public DirectorySetting  downloadsDirectory       = new DirectorySetting ("downloadsDirectory  ".trim(), new File("."                                  + File.separator + "downloads"), false, true, "Directory to put downloaded files.                                              ".trim()); { settings.add(downloadsDirectory  );  }
	public DirectorySetting  applicationDirectory     = new DirectorySetting ("applicationDirectory".trim(), new File("."                                  + File.separator + "app"      ), false, true, "Directory to put application files.                                             ".trim()); { settings.add(applicationDirectory);  }
	public DirectorySetting  stagingDirectory         = new DirectorySetting ("stagingDirectory    ".trim(), new File("."                                  + File.separator + "tempD"    ), false, true, "Directory to put files currently being served.                                  ".trim()); { settings.add(stagingDirectory    );  }
	public DirectorySetting  servingDirectory         = new DirectorySetting ("servingDirectory    ".trim(), new File("."                                  + File.separator + "tempS"    ), false, true, "Directory to put files currently being downloaded.                              ".trim()); { settings.add(servingDirectory    );  }
	public FileSetting       keysFile                 = new FileSetting      ("keysFile            ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "keys.json"), false, true, "File to put public/private key information.                                     ".trim()); { settings.add(keysFile            );  }
	public FileSetting       logFile                  = new FileSetting      ("logFile             ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "log.txt"  ), false, true, "File to put log messages if logging to file.                                    ".trim()); { settings.add(logFile             );  }
	public FileSetting       dbFile                   = new FileSetting      ("dbFile              ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "files.db" ), false, true, "File to put database.                                                           ".trim()); { settings.add(dbFile              );  }
	public FileSetting       codeUpdateKey            = new FileSetting      ("codeUpdateKey       ".trim(), new File(applicationDirectory.get().getPath() + File.separator + "updatekey"), false, true, "File containing key to authenticate code updates.                               ".trim()); { settings.add(codeUpdateKey       );  }
	
	public Settings(File settingsFile)
	{
		this.settingsFile = settingsFile;
		try
		{
			localAddress = InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e)
		{
			localAddress = "127.0.0.1";
			e.printStackTrace();
		}
		System.out.println("Local host is " + localAddress);
	}
	
	public void listenToSettings()
	{
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
		catch (IOException ex)
		{
			if (Services.logger == null)
			{
				System.out.println("No settings file found.");
			}
			else
			{
				Services.logger.println("No settings file found.");
			}
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
			Services.logger.println("Unable to save settings.");
			Services.logger.print(e);
		}
	}

    public Setting[] getUserSettings() {
        return settings.toArray(new Setting[0]);
    }

	public File getSettingsFile()
	{
		return settingsFile;
	}
}
