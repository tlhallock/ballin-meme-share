package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Properties;

import org.cnv.shr.dmn.Setting.BooleanSetting;
import org.cnv.shr.dmn.Setting.IntSetting;
import org.cnv.shr.dmn.Setting.LongSetting;
import org.cnv.shr.dmn.Setting.StringSetting;
import org.cnv.shr.util.Misc;

public class Settings
{
	public static final String encoding = "UTF8";
	public static final String checksumAlgorithm = "SHA1";
	public static final String encryptionAlgorithm = "RSA";
	
	private static final String KEYS_FILE = File.separatorChar + "keys.json";
	private static final String LOG_FILE = File.separatorChar + "log.txt";
	private static final String SQL_DIR  = "sql";
	private static final String DB_FILE  = File.separatorChar + "files.db";

	private File settingsFile;
	private String localAddress;
	private LinkedList<Setting> settings = new LinkedList<>();
	
	public StringSetting  machineName              = new StringSetting ("machineName         ".trim(), Misc.getRandomName()               , false, true, "                                          "); { settings.add(machineName         );  }
	public StringSetting  machineIdentifier        = new StringSetting ("machineIdentifier   ".trim(), Misc.getRandomString(50)           , false, true, "                                          "); { settings.add(machineIdentifier   );  }
	public StringSetting  downloadsDirectory       = new StringSetting ("downloadsDirectory  ".trim(), "downloads"                        , false, true, "                                          "); { settings.add(downloadsDirectory  );  }
	public StringSetting  applicationDirectory     = new StringSetting ("applicationDirectory".trim(), "app"                              , false, true, "                                          "); { settings.add(applicationDirectory);  }
	public StringSetting  stagingDirectory         = new StringSetting ("stagingDirectory    ".trim(), "temp"                             , false, true, "                                          "); { settings.add(stagingDirectory    );  }
	public IntSetting     defaultPort              = new IntSetting    ("defaultPort         ".trim(), 8989                               , false, true, "                                          "); { settings.add(defaultPort         );  }
	public IntSetting     servePortBegin           = new IntSetting    ("servePortBegin      ".trim(), 8990                               , false, true, "                                          "); { settings.add(servePortBegin      );  }
	public IntSetting     servePortEnd             = new IntSetting    ("servePortEnd        ".trim(), 9000                               , false, true, "                                          "); { settings.add(servePortEnd        );  }
	public IntSetting     maxDownloads             = new IntSetting    ("maxDownloads        ".trim(), 20                                 , false, true, "                                          "); { settings.add(maxDownloads        );  }
	public IntSetting     maxServes                = new IntSetting    ("maxServes           ".trim(), 20                                 , false, true, "                                          "); { settings.add(maxServes           );  }
	public IntSetting     minNaunce                = new IntSetting    ("minNaunce           ".trim(), 256                                , false, true, "                                          "); { settings.add(minNaunce           );  }
	public IntSetting     maxStringSize            = new IntSetting    ("maxStringSize       ".trim(), 4096                               , false, true, "                                          "); { settings.add(maxStringSize       );  }
	public IntSetting     numThreads               = new IntSetting    ("numThreads          ".trim(), 20                                 , false, true, "                                          "); { settings.add(numThreads          );  }
	public LongSetting    maxDirectorySize         = new LongSetting   ("maxDirectorySize    ".trim(), -1                                 , false, true, "                                          "); { settings.add(maxDirectorySize    );  }
	public LongSetting    monitorRepeat            = new LongSetting   ("monitorRepeat       ".trim(), 10 * 60 * 1000                     , false, true, "                                          "); { settings.add(monitorRepeat       );  }
	public LongSetting    checksumWait             = new LongSetting   ("checksumWait        ".trim(), 1 * 200                            , false, true, "                                          "); { settings.add(checksumWait        );  }
	public LongSetting    maxImmediateChecksum     = new LongSetting   ("maxImmediateChecksum".trim(), 1 * 1024 * 1024                    , false, true, "                                          "); { settings.add(maxImmediateChecksum);  }
	public BooleanSetting logToFile                = new BooleanSetting("logToFile           ".trim(), true                               , false, true, "                                          "); { settings.add(logToFile           );  }
	
	Settings(File settingsFile) throws UnknownHostException
	{
		this.settingsFile = settingsFile;
		localAddress = InetAddress.getLocalHost().getHostAddress();
		Services.logger.logStream.println("Local host is " + localAddress);
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
		for (Setting setting : settings)
		{
			setting.read(properties);
		}
	}
	
	public void setToDefaults() throws FileNotFoundException, IOException
	{
		for (Setting setting : settings)
		{
			setting.resetToDefaults();
		}
	}

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
