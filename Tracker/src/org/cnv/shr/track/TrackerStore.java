
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

import java.io.Closeable;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class TrackerStore implements Closeable
{
	private Connection c;

	private PreparedStatement listMachinesStatement;
	private PreparedStatement addTrackerStatement;
	private PreparedStatement listTrackersStatement;
	private PreparedStatement listAllMachinesStatement;
	private PreparedStatement listCommentsStatement;
	private PreparedStatement listFilesStatement;
	private PreparedStatement postCommentStatement;
	private PreparedStatement getMachineStatement;
	private PreparedStatement machineFoundStatement;
	private PreparedStatement machineClaimsStatement;
	private PreparedStatement machineLostStatement;
	private PreparedStatement removeMachineStatement;
	private PreparedStatement fileFoundStatement;
	private PreparedStatement cleanFilesStatement;
	private PreparedStatement getMachineIdStatement;
	private PreparedStatement removeTrackerStatement;
	private PreparedStatement listNumFiles;
//	private PreparedStatement fileRemovedStatement;
	
	public TrackerStore() throws SQLException
	{
		c = createConnection();
		
		getMachineIdStatement      = c.prepareStatement("select M_ID from MACHINE where MACHINE.IDENT = ? LIMIT 1;");
		addTrackerStatement        = c.prepareStatement("merge into TRACKER key (IP, PORT, ENDPORT) values((select T_ID from TRACKER where IP=? and PORT=? and ENDPORT=?), ?, ?, ?, ?);");
		listMachinesStatement      = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE, IDENT, KEYSTR, MNAME from MACHINE join MACHINE_CONTAINS on MACHINE_CONTAINS.MID=MACHINE.M_ID join SFILE on SFILE.F_ID=MACHINE_CONTAINS.FID where SFILE.CHKSUM=?;");
		listTrackersStatement      = c.prepareStatement("select IP, PORT, ENDPORT, LAST_ACTIVE from TRACKER;");
		listAllMachinesStatement   = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE, IDENT, KEYSTR, MNAME from MACHINE LIMIT ? OFFSET ?;");
		listCommentsStatement      = c.prepareStatement("select SENT, RATING, MESSAGE, IDENT from RATING_COMMENT join MACHINE on OID=MACHINE.M_ID where DID=(select M_ID from MACHINE where ident=?);");
		postCommentStatement       = c.prepareStatement("merge into RATING_COMMENT key (OID, DID) values((select C_ID from RATING_COMMENT where OID=? and DID=?), ?, ?, ?, ?, ?);");
		getMachineStatement        = c.prepareStatement("select IP, PORT, NPORTS, LAST_ACTIVE, KEYSTR, MNAME from MACHINE where IDENT=?;");
		machineFoundStatement      = c.prepareStatement("merge into MACHINE key (IDENT) values((select M_ID from MACHINE where IDENT=?), ?, ?, ?, ?, ?, ?, ?);");
		machineClaimsStatement     = c.prepareStatement("merge into MACHINE_CONTAINS key(FID, MID) values((select M_ID from MACHINE where IDENT=?),(select F_ID from SFILE where CHKSUM=?));");
		machineLostStatement       = c.prepareStatement("delete from MACHINE_CONTAINS where MID=(select M_ID from MACHINE where IDENT=?) and FID=(select F_ID from SFILE where CHKSUM=?);");
		listFilesStatement         = c.prepareStatement("select CHKSUM, FSIZE from SFILE join MACHINE_CONTAINS on FID=F_ID where MID=(select M_ID from MACHINE where IDENT=?);");
		removeMachineStatement     = c.prepareStatement("delete from MACHINE where IDENT=?;");
		fileFoundStatement         = c.prepareStatement("merge into SFILE key (CHKSUM) values ((select F_ID from SFILE where CHKSUM=?), ?, ?)");
		cleanFilesStatement        = c.prepareStatement("delete from SFILE where not exists (select FID from MACHINE_CONTAINS where FID=SFILE.F_ID);");
		removeTrackerStatement     = c.prepareStatement("delete from TRACKER where TRACKER.IP=? and TRACKER.PORT=?;");
		listNumFiles               = c.prepareStatement("select count(F_ID) as number from SFILE join MACHINE_CONTAINS on FID=F_ID where MID=(select M_ID from MACHINE where IDENT=?);");
//		fileRemovedStatement       = c.prepareStatement("delete from MACHINE_CONTAINS where FID=(select F_ID from SFILE where CHKSUM=?);");
	}

	// Queries
	public void listMachines(Receiver<MachineEntry> receiver, int offset)
	{
		try
		{
			listAllMachinesStatement.setInt(1, TrackerEntry.MACHINE_PAGE_SIZE);
			listAllMachinesStatement.setInt(2, offset);
			try (ResultSet results = listAllMachinesStatement.executeQuery())
			{
				MachineEntry entry = new MachineEntry();
				int count = 0;
				while (results.next())
				{
					int ndx = 1;
					String ip = results.getString(ndx++);
					int port = results.getInt(ndx++);
					int nports = results.getInt(ndx++);
					long lastActive = results.getLong(ndx++);
					String ident = results.getString(ndx++);
					String keyString = results.getString(ndx++);
					String name = results.getString(ndx++);

					entry.set(ident, keyString, ip, port, port + nports, name);
					receiver.receive(entry);
					count++;
				}

				LogWrapper.getLogger().info("Listed " + count + " machines.");
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			receiver.done();
		}
	}
	
	public void addTracker(TrackerEntry entry)
	{
		try
		{
			int ndx = 1;
			// query
			addTrackerStatement.setString(ndx++, entry.getIp());
			addTrackerStatement.setInt(ndx++, entry.getBeginPort());
			addTrackerStatement.setInt(ndx++, entry.getEndPort());
			
			// values
			addTrackerStatement.setString(ndx++, entry.getIp());
			addTrackerStatement.setInt(ndx++, entry.getBeginPort());
			addTrackerStatement.setInt(ndx++, entry.getEndPort());
			addTrackerStatement.setLong(ndx++, System.currentTimeMillis());

			addTrackerStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}
	
	public void removeTracker(TrackerEntry entry)
	{
		try
		{
			int ndx = 1;
			removeTrackerStatement.setString(ndx++, entry.getIp());
			removeTrackerStatement.setInt(ndx++, entry.getBeginPort());

			addTrackerStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}

	public void listTrackers(Receiver<TrackerEntry> listener)
	{
		try (ResultSet results = listTrackersStatement.executeQuery())
		{
			TrackerEntry tracker = new TrackerEntry();
			while (results.next())
			{
				int ndx = 1;
				String ip = results.getString(ndx++);
				int port  = results.getInt(ndx++);
				int endport = results.getInt(ndx++);
				long lastActive = results.getLong(ndx++);
				
				tracker.set(ip, port, endport);
				listener.receive(tracker);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			listener.done();
		}
	}
	
	public void listSeeders(FileEntry entry, Receiver<MachineEntry> output)
	{
		try
		{
			listMachinesStatement.setString(1, entry.getChecksum());
			try (ResultSet results = listMachinesStatement.executeQuery())
			{
				MachineEntry machineEntry = new MachineEntry();
				while (results.next())
				{
					int ndx = 1;
					String ip = results.getString(ndx++);
					int port = results.getInt(ndx++);
					int nports = results.getInt(ndx++);
					long lastActive = results.getLong(ndx++);
					String ident = results.getString(ndx++);
					String keyString = results.getString(ndx++);
					String name = results.getString(ndx++);

					machineEntry.set(ident, keyString, ip, port, port + nports, name);
					output.receive(machineEntry);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			output.done();
		}
	}
	
	public void listComments(MachineEntry entry, Receiver<CommentEntry> generator)
	{
		String did = entry.getIdentifer();
		
		try
		{
			listCommentsStatement.setString(1, did);
			try (ResultSet results = listCommentsStatement.executeQuery())
			{
				CommentEntry comment = new CommentEntry();
				while (results.next())
				{
					int ndx = 1;
					long sent = results.getLong(ndx++);
					int rating = results.getInt(ndx++);
					String message = results.getString(ndx++);
					String oid = results.getString(ndx++);

					comment.set(oid, did, message, rating, sent);
					generator.receive(comment);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			generator.done();
		}
	}
	
	public int getMachineId(String ident)
	{
		try
		{
			getMachineIdStatement.setString(1, ident);
			try (ResultSet results = getMachineIdStatement.executeQuery())
			{
				if (!results.next())
				{
					return -1;
				}
				return results.getInt(1);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		return -1;
	}

	public MachineEntry getMachine(String ident)
	{
		try
		{
			getMachineStatement.setString(1, ident);
			try (ResultSet results = getMachineStatement.executeQuery())
			{
				if (!results.next())
				{
					return null;
				}
				int ndx = 1;
				String ip = results.getString(ndx++);
				int port = results.getInt(ndx++);
				int nports = results.getInt(ndx++);
				long lastActive = results.getLong(ndx++);
				String keyString = results.getString(ndx++);
				String name = results.getString(ndx++);

				return new MachineEntry(ident, keyString, ip, port, port + nports, name);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		return null;
	}
	
	public void listFiles(MachineEntry entry, Receiver<FileEntry> output)
	{
		try
		{
			listFilesStatement.setString(1, entry.getIdentifer());
			try (ResultSet results = listFilesStatement.executeQuery())
			{
				FileEntry fileEntry = new FileEntry();
				while (results.next())
				{
					int ndx = 1;
					String checksum = results.getString(ndx++);
					long size = results.getLong(ndx++);

					fileEntry.set(checksum, size);
					output.receive(fileEntry);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		finally
		{
			output.done();
		}
	}

	public Long getNumFiles(MachineEntry entry)
	{
		try
		{
			listNumFiles.setString(1, entry.getIdentifer());
			try (ResultSet results = listNumFiles.executeQuery())
			{
				if (!results.next())
				{
					return null;
				}
				int ndx = 1;
				long long1 = results.getLong(ndx++);
				if (long1 > 0)
				{
					return long1;
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		return null;
	}
	
	public void postComment(CommentEntry comment)
	{
		int oid = getMachineId(comment.getOrigin());
		int did = getMachineId(comment.getDestination());
		if (oid < 0 || did < 0)
		{
			LogWrapper.getLogger().info("Unable to find machines for comment " + comment);
			return;
		}
		try
		{
		  int ndx = 1;
		  postCommentStatement.setInt   (ndx++, oid);
		  postCommentStatement.setInt   (ndx++, did);
		  postCommentStatement.setInt   (ndx++, oid);
		  postCommentStatement.setInt   (ndx++, did);
		  postCommentStatement.setLong  (ndx++, comment.getDate());
		  postCommentStatement.setInt   (ndx++, comment.getRating());
		  postCommentStatement.setString(ndx++, comment.getText());

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
			machineFoundStatement.setString(ndx++, machine.getName());
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

	public void machineClaims(MachineEntry machine, FileEntry file)
	{
		try
		{
			c.setAutoCommit(false);
			try
			{
				int ndx = 1;
				fileFoundStatement.setString(ndx++, file.getChecksum());
				fileFoundStatement.setLong(  ndx++, file.getFileSize());
				fileFoundStatement.setString(ndx++, file.getChecksum());
				fileFoundStatement.execute();
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
			}
			try
			{
				int ndx = 1;
				machineClaimsStatement.setString(ndx++, machine.getIdentifer());
				machineClaimsStatement.setString(ndx++, file.getChecksum());
				machineClaimsStatement.execute();
				c.commit();
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
			}
		}
		catch (SQLException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e1);
		}
		finally
		{
			try
			{
				c.setAutoCommit(true);
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
			}
		}
	}
	
	public void machineLost(MachineEntry machine, FileEntry file)
	{
		try
		{
			int ndx = 1;
			machineLostStatement.setString(ndx++, machine.getIdentifer());
			machineLostStatement.setString(ndx++, file.getChecksum());
			machineLostStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
		cleanFiles();
	}
	
	public static Connection createConnection() throws SQLException
	{
//		return DriverManager.getConnection("jdbc:h2:" + Paths.get("tracker_store").toAbsolutePath(), "sa", "");
		Path path = Track.rootDirectory.resolve("tracker_store");
		Misc.ensureDirectory(path, true);
		return DriverManager.getConnection("jdbc:h2:" + path.toAbsolutePath(), "sa", "");
	}

	public void cleanFiles()
	{
		try
		{
			cleanFilesStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}

	public void removeMachine(MachineEntry entry1)
	{
		try
		{
			removeMachineStatement.setString(1, entry1.getIdentifer());
			removeMachineStatement.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to execute query.", e);
		}
	}

	public void debug(String tableName)
	{
		Misc.debugTable(tableName, c);
	}

	@Override
	public void close()
	{
		try { getMachineIdStatement   .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"getMachineIdStatement   \"", ex); }
		try { addTrackerStatement     .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"addTrackerStatement     \"", ex); }
		try { listMachinesStatement   .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"listMachinesStatement   \"", ex); }
		try { listTrackersStatement   .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"listTrackersStatement   \"", ex); }
		try { listAllMachinesStatement.close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"listAllMachinesStatement\"", ex); }
		try { listCommentsStatement   .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"listCommentsStatement   \"", ex); }
		try { postCommentStatement    .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"postCommentStatement    \"", ex); }
		try { getMachineStatement     .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"getMachineStatement     \"", ex); }
		try { machineFoundStatement   .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"machineFoundStatement   \"", ex); }
		try { machineClaimsStatement  .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"machineClaimsStatement  \"", ex); }
		try { machineLostStatement    .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"machineLostStatement    \"", ex); }
		try { listFilesStatement      .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"listFilesStatement      \"", ex); }
		try { removeMachineStatement  .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"removeMachineStatement  \"", ex); }
		try { fileFoundStatement      .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"fileFoundStatement      \"", ex); }
		try { cleanFilesStatement     .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"cleanFilesStatement     \"", ex); }
		try { listNumFiles            .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close statement \"listNumFiles            \"", ex); }
		try { c                       .close(); } catch (Exception ex) { LogWrapper.getLogger().log(Level.INFO, "Unable to close connection.", ex); }
	}
}
