
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Properties;
import java.util.logging.Level;

import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;

public class UpdateInfoImpl implements UpdateInfo
{
	private Path propsFile;
	private Path keysFile;
	
	private long timeStamp;
	private Properties props;
	private KeysService keys;
	public static final String KEYS_TXT = "keys.txt";
	public static final String INFO_PROPS = "info.props";
	
	public UpdateInfoImpl(String updateManagerRoot) throws Exception
	{
		this.propsFile = Paths.get(updateManagerRoot + File.separator + propsFile);
		this.keysFile  = Paths.get(updateManagerRoot + File.separator + keysFile );
		
		if (!Files.exists(propsFile) || !Files.exists(keysFile))
		{
			throw new Exception("Updater not running!");
		}
		
		props = new Properties();
		keys = new KeysService();
		keys.readKeys(keysFile, -1);
	}
	
	
	@Override
	public String getIp()
	{
		return props.getProperty("ip");
	}

	@Override
	public int getPort()
	{
		return Integer.parseInt(props.getProperty("port"));
	}

	@Override
	public PrivateKey getPrivateKey(PublicKey usedKey)
	{
		return keys.getPrivateKey(usedKey);
	}

	@Override
	public PublicKey getLatestPublicKey()
	{
		return keys.getPublicKey();
	}
	
	private synchronized void checkTime()
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
	
	public static void write(Path propsFile, String ip, int port) throws FileNotFoundException, IOException
	{
		Properties props = new Properties();
		props.setProperty("ip", ip);
		props.setProperty("port", String.valueOf(port));
		
		try (OutputStream fileOutputStream = Files.newOutputStream(propsFile);)
		{
			props.store(fileOutputStream, "No comment.");
		}
	}
}
