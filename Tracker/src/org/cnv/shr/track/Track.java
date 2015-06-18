
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



package org.cnv.shr.track;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.cnv.shr.dmn.trk.TrackerGui;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class Track
{
	static KeysService keys;
	static Timer timer;
	public static ExecutorService threads;
	static TrackerGui gui;
	public static TrackerEntry LOCAL_TRACKER = getLocalTracker();
	public static Path rootDirectory = Paths.get(/*"..", "instances",*/ "tracker");
	public static boolean storesMetaData = true;
	
	
	private static void parseArgs(String[] args)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-d") && i < args.length - 1)
			{
				rootDirectory = Paths.get(args[i + 1]);
			}
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException, UnknownHostException
	{
		parseArgs(args);
		Misc.ensureDirectory(rootDirectory, false);
		LogWrapper.logToFile(rootDirectory.resolve("tracker_log.txt"), 1024 * 1024);
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException(Thread t, Throwable e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Uncaught exception.", e);
			}
		});
		
		
		LogWrapper.getLogger().info("Address: " + InetAddress.getLocalHost().getHostAddress());
		
		threads = Executors.newCachedThreadPool();
		timer = new Timer();
		
		Class.forName("org.h2.Driver");
//		deleteDb();
		createDb();
		keys = new KeysService();
		
		ensureLocalTrackerIsPresent();
		
		gui = new TrackerGui();
		gui.setVisible(true);
		
		for (int port = TrackerEntry.TRACKER_PORT_BEGIN; port < TrackerEntry.TRACKER_PORT_END; port++)
		{
			try
			{
				new Thread(new Tracker(new ServerSocket(port))).start();
			}
			catch (SQLException | IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to start on port " + port, e);
			}
		}
	}

	private static TrackerEntry getLocalTracker()
	{
		try
		{
			return new TrackerEntry(
					InetAddress.getLocalHost().getHostAddress(),
					TrackerEntry.TRACKER_PORT_BEGIN, TrackerEntry.TRACKER_PORT_END);
		}
		catch (UnknownHostException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get local host.", e);
			System.exit(-1);
			return null;
		}
	}
	
	private static void ensureLocalTrackerIsPresent() throws UnknownHostException, SQLException
	{
		try (TrackerStore store = new TrackerStore())
		{
			store.addTracker(LOCAL_TRACKER);
		}
	}

	static void createDb() throws SQLException
	{
		try (Connection c = TrackerStore.createConnection())
		{
			String[] statements = Misc.readFile(Settings.RES_DIR + "create.sql").split(";");
			for (int i = 0; i < statements.length; i++)
			{
				try (PreparedStatement stmt = c.prepareStatement(statements[i] + ";");)
				{
					LogWrapper.getLogger().fine("Executing " + statements[i]);
					stmt.execute();
				}
			}
		}
	}
	
	static void deleteDb() throws SQLException
	{
		try (Connection c = TrackerStore.createConnection())
		{                                 
			try (PreparedStatement stmt = c.prepareStatement("drop table MACHINE            ;");) { stmt.execute(); }
			try (PreparedStatement stmt = c.prepareStatement("drop table TRACKER            ;");) { stmt.execute(); }
			try (PreparedStatement stmt = c.prepareStatement("drop table SFILE              ;");) { stmt.execute(); }
			try (PreparedStatement stmt = c.prepareStatement("drop table MACHINE_CONTAINS   ;");) { stmt.execute(); }
			try (PreparedStatement stmt = c.prepareStatement("drop table RATING_COMMENT     ;");) { stmt.execute(); }
		}
	}

	private static boolean portsAreSane(int begin, int end)
	{
		return begin < end && begin > 0 && end < 100000;
	}
}
