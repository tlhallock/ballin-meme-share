package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.dmn.Services;

public class Download extends DbObject<Integer>
{
	private RemoteFile file;
	private SharedFileState currentState;
	private long added;
	
	public Download(RemoteFile remote)
	{
		super(null);
		this.file = remote;
		this.currentState = SharedFileState.DOWNLOADING;
		this.added = System.currentTimeMillis();
	}

	public Download(int int1)
	{
		super(int1);
	}
	
	public RemoteFile getFile()
	{
		return file;
	}

	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		int ndx = 1;
		this.id = row.getInt(ndx++);
		this.file = (RemoteFile) DbFiles.getFile(row.getInt(ndx++));
		this.added = row.getLong(ndx++);
		this.currentState = SharedFileState.getState(row.getInt(ndx++));
	}
	public void remove()
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				"delete from PENDING_DOWNLOAD where FID=?");)
		{
			stmt.setInt(1, file.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public boolean save(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				"merge into PENDING_DOWNLOAD key(FID) values ((select Q_ID from PENDING_DOWNLOAD where FID=?), ?, ?, ?)");)
		{
			int ndx = 1;
			stmt.setInt(ndx++, file.getId());
			stmt.setInt(ndx++, file.getId());
			stmt.setLong(ndx++, added);
			stmt.setInt(ndx++, currentState.dbValue);
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				id = generatedKeys.getInt(1);
				return true;
			}
			return false;
		}
	}
	public enum SharedFileState
	{
		LOCAL          (0),
		REMOTE         (1),
		QUEUED         (2),
		DOWNLOADING    (3),
		DOWNLOADED     (4),
		HAVE_COPY      (5),
		
		;
		
		int dbValue;
		
		SharedFileState(int value)
		{
			dbValue = value;
		}
		
		public int toInt()
		{
			return dbValue;
		}
		
		static SharedFileState getState(int dbValue)
		{
			for (SharedFileState s : SharedFileState.values())
			{
				if (s.dbValue == dbValue)
				{
					return s;
				}
			}
			Services.logger.println("Unknown file state: " + dbValue);
			return null;
		}
	}
}
