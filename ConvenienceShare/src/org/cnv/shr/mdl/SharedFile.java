package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbTables;

public abstract class SharedFile extends DbObject<Integer>
{
	protected RootDirectory rootDirectory;
	protected PathElement path;
	protected long fileSize;
	protected String checksum;
	protected String tags;
	protected long lastModified;
	

	public SharedFile(Integer int1) {
		super(int1);
	}

	public String getRelativePath()
	{
		String fpath = path.getFullPath();
		int index = fpath.lastIndexOf('/');
		if (index < 0)
		{
			fpath = fpath.substring(index + 1);
		}
		return "";
	}

	public PathElement getPath()
	{
		return path;
	}

	public RootDirectory getRootDirectory()
	{
		return rootDirectory;
	}

	public void setRootDirectory(RootDirectory rootDirectory)
	{
		this.rootDirectory = rootDirectory;
	}

	public long getFileSize()
	{
		return fileSize;
	}

	public void setFileSize(long filesize)
	{
		this.fileSize = filesize;
	}

	public String getChecksum()
	{
		return checksum;
	}

	public void setChecksum(String checksum)
	{
		this.checksum = checksum;
	}

	public long getLastUpdated()
	{
		return lastModified;
	}

	public void setLastUpdated(long long1)
	{
		this.lastModified = long1;
	}

	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		id = row.getInt("F_ID");

		rootDirectory =  (RootDirectory) locals.getObject(c, DbTables.DbObjects.LROOT, row.getInt("ROOT"));
		path          =    (PathElement) locals.getObject(c, DbTables.DbObjects.PELEM, row.getInt("PELEM"));
		
		tags = row.getString("TAGS");
		fileSize = row.getLong("FSIZE");
		checksum = row.getString("CHKSUM");
		lastModified = row.getLong("MODIFIED");
	}


	@Override
	public boolean save(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement(
				 "merge into SFILE key(PELEM, ROOT) values ((select F_ID from SFILE where PELEM=? and ROOT=?), ?, ?, ?, ?, ?, ?, ?, ?);"
				, Statement.RETURN_GENERATED_KEYS);)
		{
			int ndx=1;
			stmt.setLong(ndx++, path.getId());
			stmt.setInt(ndx++, rootDirectory.getId());
			
			stmt.setLong(ndx++, getFileSize());
			stmt.setString(ndx++, getTags());
			stmt.setString(ndx++, checksum);
			stmt.setLong(ndx++, path.getId());
			stmt.setInt(ndx++, rootDirectory.getId());
			stmt.setInt(ndx++, 0/*remote state*/);
			stmt.setLong(ndx++, lastModified);
			stmt.setInt(ndx++, 0 /*error*/);
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
	

//	@Override
//	public void read(InputStream bytes) throws IOException
//	{
////		path =         (ByteReader.readString(bytes));
////		fileSize =     (ByteReader.readLong  (bytes));
////		tags =         (ByteReader.readString(bytes));
////		checksum =     (ByteReader.readString(bytes));
////		lastModified = (ByteReader.readLong  (bytes));
//	}
//
//	@Override
//	public void write(ByteListBuffer buffer)
//	{
////		buffer.append(getName());
////		buffer.append(getRelativePath());
////		buffer.append(getFileSize());
////		buffer.append(getTags());
////		buffer.append(getChecksum());
////		buffer.append(getLastUpdated());
//	}

	public String getTags()
	{
		return tags;
	}

	public abstract boolean isLocal();
}
