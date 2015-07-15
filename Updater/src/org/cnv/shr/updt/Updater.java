
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

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class Updater
{
	private static final long A_LONG_TIME = 7 * 24 * 60 * 60 * 1000;
	
	static int KEY_LENGTH = 1024;
	static String ROOT_DIRECTORY = 
			"../instances/" + 
			"updater/";
	
	static Path keysFile;
	static Path propsFile;

	static ServerSocket updateSocket;
	
	static KeysService service; 
	static UpdateThread updateThread;
	static Code code;
	static CodeMonitor monitor;
	
	static Path getUpdatesDirectory()
	{
		return Paths.get(ROOT_DIRECTORY, "updates/");
	}
	
	public static void main(String[] args) throws Exception
	{
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException(Thread t, Throwable e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Uncaught exception.", e);
			}
		});
		if (args.length > 0)
		{
			ROOT_DIRECTORY = args[0];
		}
		Misc.ensureDirectory(getUpdatesDirectory(), false);
		
		LogWrapper.getLogger().info("Root directory is " + ROOT_DIRECTORY);

		keysFile  = Paths.get(ROOT_DIRECTORY, UpdateInfoImpl.KEYS_TXT);
		propsFile = Paths.get(ROOT_DIRECTORY, UpdateInfoImpl.INFO_PROPS);
		
		updateSocket = new ServerSocket(UpdateInfo.DEFAULT_UPDATE_PORT);
		code = new Code();
		code.checkTime();
		service = new KeysService();
		service.readKeys(keysFile, KEY_LENGTH);
		updateThread = new UpdateThread();
		updateProps();
		updateThread.start();
		
		Misc.timer.scheduleAtFixedRate(new KeyUpdater(), A_LONG_TIME, A_LONG_TIME);
		
		monitor = new CodeMonitor(getUpdatesDirectory());
		new Thread(monitor).start();
	}
	
	public static void updateProps()
	{
		try
		{
			Misc.ensureDirectory(propsFile, true);
			UpdateInfoImpl.write(propsFile, updateSocket.getInetAddress().getHostAddress(), updateSocket.getLocalPort(), code.getVersion());
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save props", e);
		}
	}
}
