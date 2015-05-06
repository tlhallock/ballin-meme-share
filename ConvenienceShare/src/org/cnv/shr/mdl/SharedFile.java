package org.cnv.shr.mdl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.util.ByteListBuffer;

public abstract class SharedFile extends DbObject implements NetworkObject
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

		rootDirectory = (LocalDirectory) locals.getObject(c, DbTables.DbObjects.LROOT, row.getInt("ROOT"));
		path          =    (PathElement) locals.getObject(c, DbTables.DbObjects.PELEM, row.getInt("PELEM"));
		
		tags = row.getString("TAGS");
		fileSize = row.getLong("FSIZE");
		checksum = row.getString("CHKSUM");
		lastModified = row.getLong("MODIFIED");
	}

	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
		PreparedStatement stmt = c.prepareStatement(
				 "merge into SFILE key(PELEM, ROOT) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?);"
				, Statement.RETURN_GENERATED_KEYS);
		int ndx = 1;
		stmt.setLong(ndx++, getFileSize());
		stmt.setString(ndx++, getTags());
		stmt.setString(ndx++, checksum);
		stmt.setInt(ndx++, path.getId());
		stmt.setInt(ndx++, rootDirectory.getId());
		stmt.setInt(ndx++, 0/*remote state*/);
		stmt.setLong(ndx++, lastModified);
		stmt.setInt(ndx++, 0 /*error*/);
		return stmt;
	}

	@Override
	public void read(InputStream bytes) throws IOException
	{
//		path =         (ByteReader.readString(bytes));
//		fileSize =     (ByteReader.readLong  (bytes));
//		tags =         (ByteReader.readString(bytes));
//		checksum =     (ByteReader.readString(bytes));
//		lastModified = (ByteReader.readLong  (bytes));
	}

	@Override
	public void write(ByteListBuffer buffer)
	{
//		buffer.append(getName());
//		buffer.append(getRelativePath());
//		buffer.append(getFileSize());
//		buffer.append(getTags());
//		buffer.append(getChecksum());
//		buffer.append(getLastUpdated());
	}

	public String getTags()
	{
		return tags;
	}

	public abstract boolean isLocal();
}
