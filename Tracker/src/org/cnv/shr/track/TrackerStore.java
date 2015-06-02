package org.cnv.shr.track;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;

import org.cnv.shr.trck.Comment;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class TrackerStore
{
	private Connection c;

	private PreparedStatement listMachinesStatement;
	private PreparedStatement listTrackersStatement;
	private PreparedStatement listAllMachinesStatement;
	private PreparedStatement listCommentsStatement;
	private PreparedStatement listFilesStatement;
	private PreparedStatement postCommentStatement;
	private PreparedStatement getMachineStatement;
	private PreparedStatement machineFoundStatement;
	private PreparedStatement machineClaimsStatement;
	private PreparedStatement machineLostStatement;
	private PreparedStatement machineVerifiedStatement;

	private PreparedStatement getMachineIdStatement;
	
	
	TrackerStore() throws SQLException
	{
		c = createConnection();
		
		getMachineIdStatement      = c.prepareStatement("select M_ID from MACHINE where MACHINE.IDENT = ?;");
		
		listMachinesStatement      = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE, IDENT, KEYSTR from MACHINE join MACHINE_CONTAINS on MACHINE_CONTAINS.MID=MACHINE.M_ID join SFILE on SFILE.F_ID=MACHINE_CONTAINS.FID where SFILE.CHKSUM=?;");
		listTrackersStatement      = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE from TRACKER;");
		listAllMachinesStatement   = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE, IDENT, KEYSTR from MACHINE;");
		listCommentsStatement      = c.prepareStatement("select SENT, RATING, MESSAGE, IDENT from RATING_COMMENT join MACHINE on OID=MACHINE.M_ID where DID=?;");
		postCommentStatement       = c.prepareStatement("merge into RATING_COMMENT key (OID, DID) values((select C_ID from RATING_COMMENT where OID=? and DID=?), ?, ?, ?, ?, ?);");
		getMachineStatement        = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE, KEYSTR from MACHINE where IDENT=?;");
		machineFoundStatement      = c.prepareStatement("merge into MACHINE key (IDENT) values((select M_ID from MACHINE where IDENT=?), ?, ?, ?, ?, ?, ?, ?);");
		machineClaimsStatement     = c.prepareStatement("");
		machineLostStatement       = c.prepareStatement("delete FROM MACHINE_CONTAINS where MID=? and FID=?;");
		listFilesStatement         = c.prepareStatement("select CHKSUM, FSIZE from SFILE join MACHINE_CONTAINS on FID=F_ID where MID=?;");
		machineVerifiedStatement   = c.prepareStatement("");
	}

	// Queries
	public void listMachines(JsonGenerator output)
	{
		output.writeStartArray();
		try (ResultSet results = listAllMachinesStatement.executeQuery())
		{
			MachineEntry entry = new MachineEntry();
			while (results.next())
			{
				int ndx = 1;
				String ip = results.getString(ndx++);
				int port  = results.getInt(ndx++);
				int nports = results.getInt(ndx++);
				long lastActive = results.getLong(ndx++);
				String ident = results.getString(ndx++);
				String keyString = results.getString(ndx++);
				
				entry.set(ident, keyString, ip, port, port + nports);
				entry.print(output);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			output.writeEnd();
		}
	}
	
	public void listTrackers(JsonGenerator output)
	{
		output.writeStartArray();
		try (ResultSet results = listTrackersStatement.executeQuery())
		{
			TrackerEntry tracker = new TrackerEntry();
			while (results.next())
			{
				int ndx = 1;
				String ip = results.getString(ndx++);
				int port  = results.getInt(ndx++);
				int nports = results.getInt(ndx++);
				long lastActive = results.getLong(ndx++);
				
				tracker.set(ip, port, port + nports);
				tracker.print(output);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			output.writeEnd();
		}
	}
	
	public void listMachines(FileEntry entry, JsonGenerator output)
	{
		output.writeStartArray();
		try (ResultSet results = listMachinesStatement.executeQuery())
		{
			MachineEntry machineEntry = new MachineEntry();
			while (results.next())
			{
				int ndx = 1;
				String ip = results.getString(ndx++);
				int port  = results.getInt(ndx++);
				int nports = results.getInt(ndx++);
				long lastActive = results.getLong(ndx++);
				String ident = results.getString(ndx++);
				String keyString = results.getString(ndx++);

				machineEntry.set(ident, keyString, ip, port, port + nports);
				machineEntry.print(output);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			output.writeEnd();
		}
	}
	
	public void listComments(MachineEntry entry, JsonGenerator generator)
	{
		String did = entry.getIdentifer();
		generator.writeStartArray();
		try (ResultSet results = listCommentsStatement.executeQuery())
		{
			Comment comment = new Comment();
			while (results.next())
			{
				int ndx = 1;
				long sent = results.getLong(ndx++);
				int rating = results.getInt(ndx++);
				String message = results.getString(ndx++);
				String oid = results.getString(ndx++);
				
				comment.set(oid, did, message, rating, sent);
				comment.print(generator);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			generator.writeEnd();
		}
	}
	
	public MachineEntry getMachine(String ident)
	{
		try (ResultSet results = getMachineStatement.executeQuery())
		{
			if (results.next())
			{
				int ndx = 1;
				String ip = results.getString(ndx++);
				int port  = results.getInt(ndx++);
				int nports = results.getInt(ndx++);
				long lastActive = results.getLong(ndx++);
				String keyString = results.getString(ndx++);
				
				return new MachineEntry(ident, keyString, ip, port, port + nports);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		return null;
	}
	
	public void listFiles(MachineEntry entry, JsonGenerator output)
	{
		output.writeStartArray();
		try (ResultSet results = listFilesStatement.executeQuery())
		{
			FileEntry fileEntry = new FileEntry();
			while (results.next())
			{
				int ndx = 1;
				String checksum = results.getString(ndx++);
				long size = results.getLong(ndx++);

				fileEntry.set(checksum, size);
				fileEntry.print(output);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			output.writeEnd();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	
	public void postComment(Comment comment)
	{
		try
		{
			postCommentStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}
	
	public void machineFound(MachineEntry machine, long now)
	{
		try
		{
			int ndx = 1;
			machineFoundStatement.setString(ndx++, machine.getIdentifer());
			
			
			machineFoundStatement.setString(ndx++, machine.getIdentifer());
			machineFoundStatement.setString(ndx++, "No name");
			machineFoundStatement.setString(ndx++, machine.getIp());
			machineFoundStatement.setInt   (ndx++, machine.getPortBegin());
			machineFoundStatement.setInt   (ndx++, machine.getPortEnd() - machine.getPortBegin());
			machineFoundStatement.setLong  (ndx++, now);
			machineFoundStatement.setString(ndx++, machine.getKeyStr());
			
			machineFoundStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}
	// entries
	public void machineClaims(MachineEntry machine, FileEntry file)
	{
		try
		{
			machineClaimsStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}
	
	public void machineLost(MachineEntry machine, FileEntry file)
	{
		try
		{
			machineLostStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}
	

	public void machineValidated(MachineEntry claimedClient, long currentTimeMillis)
	{
		try
		{
			machineVerifiedStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	


	public static Connection createConnection() throws SQLException
	{
//		return DriverManager.getConnection("jdbc:h2:" + Paths.get("tracker_store").toAbsolutePath(), "sa", "");
		Path path = Paths.get("..", "instances", "tracker", "tracker_store");
		Misc.ensureDirectory(path, true);
		return DriverManager.getConnection("jdbc:h2:" + path.toAbsolutePath(), "sa", "");
	}
}
