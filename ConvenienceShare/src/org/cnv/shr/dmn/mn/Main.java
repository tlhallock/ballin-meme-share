
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



package org.cnv.shr.dmn.mn;

import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.SplashScreen;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.ShowApplication;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.LogWrapper;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		SplashScreen screen = SplashScreen.showSplash();
		Arguments a = new Arguments();
		a.parseArgs(args);
		Settings settings;
		
		a.settings = new Settings(Paths.get("another\\apps\\settings.props"));
		
//		a.settings = new Settings(Paths.get("/work/ballin-meme-share/instances/i1/settings.props"));
		a.settings.setDefaultApplicationDirectoryStructure();
		a.showGui = true;

		System.out.println("Settings file: " + a.settings.getSettingsFile());
		
		if (Services.isAlreadyRunning(a, screen))
		{
			screen.setStatus("ConvenienceShare is already running!!! Will close soon.");
			
			LogWrapper.getLogger().info("Application must already be running.");
			String address = InetAddress.getLocalHost().getHostAddress();
			try (Socket socket = new Socket(address, a.settings.servePortBeginI.get());
					JsonParser input = TrackObjectUtils.createParser(socket.getInputStream());
					JsonGenerator outputStream = TrackObjectUtils.createGenerator(socket.getOutputStream());)
			{
				ShowApplication showApplication = new ShowApplication();
				DoneMessage doneMessage = new DoneMessage();
				outputStream.writeStartObject();
				showApplication.generate(outputStream, showApplication.getJsonKey());
				doneMessage.generate(outputStream, doneMessage.getJsonKey());
				outputStream.writeEnd();
				outputStream.flush();
				LogWrapper.getLogger().info("Message sent. Waiting...");
				Thread.sleep(5000);
			}
			return;
		}
		
		try
		{
			Services.initialize(a, screen);
		}
		catch (Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to initialize", ex);
			if (Services.quiter != null)
			{
				Services.quiter.quit();
			}
		}
		
		if (a.launchOnStart)
		{
			RunOnStartUp.runOnStartup();
		}
//		for (;;)
//		{
//			synchronized (System.out)
//			{
//				Thread.sleep(1000);
//			}
//			Thread.sleep(1000);
//		}
	}

	public static void restart(LinkedList<String> args)
	{
		Restart restart = new Restart();
		restart.setArgs(args);
		Services.quiter = restart;
		Services.quiter.quit();
	}
	
	public static void restart()
	{
		restart(new LinkedList<String>());
	}
}
