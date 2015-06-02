package org.cnv.shr.track;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Timer;
import java.util.logging.Level;

import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class Track
{
	static int TRACKER_PORT_BEGIN = 9001;
	static int TRACKER_PORT_END   = 9002;
	
	static KeysService keys;
	static Timer timer;

	public static void main(String[] args) throws ClassNotFoundException, SQLException
	{
		timer = new Timer();
		
		Class.forName("org.h2.Driver");
		createDb();
		keys = new KeysService();
		
		for (int port = TRACKER_PORT_BEGIN; port < TRACKER_PORT_END; port++)
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

	static void createDb() throws SQLException
	{
		try (Connection c = TrackerStore.createConnection())
		{
			String[] statements = Misc.readFile(Settings.RES_DIR + "create.sql").split(";");
			for (int i = 0; i < statements.length; i++)
			{
				try (PreparedStatement stmt = c.prepareStatement(statements[i] + ";");)
				{
					System.out.println("Executing " + statements[i]);
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
