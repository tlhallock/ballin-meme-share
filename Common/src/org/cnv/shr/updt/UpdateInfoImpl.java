
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.updt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.json.JsonStringMap;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.MissingKeyException;

public class UpdateInfoImpl implements UpdateInfo
{
	private Path propsFile;
	private Path keysFile;
	private Path versionsFile;
	private JsonStringMap lastKnownVersions = new JsonStringMap();
	
	private long timeStamp;
	private Properties props;
	private KeysService keys;
	public static final String KEYS_TXT = "keys.txt";
	public static final String INFO_PROPS = "info.props";
	public static final String VERSIONS_TXT = "versions.txt";
	
	public UpdateInfoImpl(Path updateManagerRoot) throws Exception
	{
		this.propsFile    = updateManagerRoot.resolve(INFO_PROPS);
		this.keysFile     = updateManagerRoot.resolve(KEYS_TXT);
		this.versionsFile = updateManagerRoot.resolve(VERSIONS_TXT);
		
		if (propsFile == null || keysFile == null || versionsFile == null || !Files.exists(propsFile) || !Files.exists(keysFile))
		{
			throw new Exception("Updater not running!");
		}
		
		props = new Properties();
		keys = new KeysService();
		keys.readKeys(keysFile, -1);
		checkTime();
		readLastKnownVersions();
	}
	
	
	@Override
	public String getIp()
	{
		return props.getProperty("ip");
	}

	@Override
	public int getPort()
	{
		String prop = props.getProperty("port");
		try
		{
			return Integer.parseInt(prop);
		}
		catch (NumberFormatException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get port from " + prop, ex);
			return -1;
		}
	}

	@Override
	public PrivateKey getPrivateKey(PublicKey usedKey) throws MissingKeyException
	{
		return keys.getPrivateKey(usedKey);
	}

	@Override
	public PublicKey getLatestPublicKey()
	{
		return keys.getPublicKey();
	}
	
	@Override 
	public long getLastKeyTimeStamp()
	{
		return keys.getLastKeyTimeStamp();
	}
	
	public synchronized void checkTime()
	{
		long fsTime = Math.max(propsFile.toFile().lastModified(), keysFile.toFile().lastModified());
		if (fsTime < timeStamp)
		{
			return;
		}
		read(fsTime);
	}

	private void read(long ts)
	{
		try (InputStream input = Files.newInputStream(propsFile))
		{
			Properties newProps = new Properties();
			newProps.load(input);
			props = newProps;
			timeStamp = ts;
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read update manager info.", e);
		}
		try
		{
			keys.readKeys(keysFile, -1);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read keys file.", e);
		}
	}
	
	public static void write(Path propsFile, String ip, int port, String string) throws FileNotFoundException, IOException
	{
		Properties props = new Properties();
		props.setProperty("ip", ip);
		props.setProperty("port", String.valueOf(port));
		if (string == null)
		{
			props.setProperty("servingVersion", "None found.");
		}
		else
		{
			props.setProperty("servingVersion", string);
		}
		
		try (OutputStream fileOutputStream = Files.newOutputStream(propsFile);)
		{
			props.store(fileOutputStream, "No comment.");
		}
	}

	
	private void readLastKnownVersions()
	{
		try (JsonParser generator = TrackObjectUtils.createParser(Files.newInputStream(versionsFile), false);)
		{
			lastKnownVersions.parse(generator);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read known versionCache from " + versionsFile, e);
			writelastKnownVersions();
		}
	}
	private void writelastKnownVersions()
	{
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(versionsFile), true))
		{
			generator.writeStartObject();
			lastKnownVersions.generate(generator);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write known versionCache to " + versionsFile, e);
		}
	}

	@Override
	public String getLastKnownVersion(String identifier)
	{
		String string = lastKnownVersions.get(identifier);
		if (string == null)
		{
			return "Unkown";
		}
		return string;
	}


	@Override
	public void setLastKnownVersion(String identifier, String version)
	{
		lastKnownVersions.put(identifier, version);
		writelastKnownVersions();
	}


	@Override
	public void useNewKey() throws NoSuchAlgorithmException, NoSuchProviderException, IOException
	{
		keys.createAnotherKey(keysFile, 1024);
	}

	public String getVersionOfCodeServing()
	{
		return props.getProperty("servingVersion");
	}
}
