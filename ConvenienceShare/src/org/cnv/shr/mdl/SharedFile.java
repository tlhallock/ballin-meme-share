package org.cnv.shr.mdl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class SharedFile extends DbObject implements NetworkObject
{
	protected String name;
	protected String path;
	protected RootDirectory rootDirectory;
	protected long fileSize;
	protected String checksum;
	protected String tags;
	protected long lastModified;
	

	public SharedFile(Integer int1) {
		super(int1);
	}

	public String getName()
	{
		return name;
	}

	public String getRelativePath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
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

	public void setName(String name)
	{
		this.name = name;
	}

	public void setLastUpdated(long long1)
	{
		this.lastModified = long1;
	}

	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		id = row.getInt("F_ID");
		
		String path     = DbPaths.getPath(c, row.getInt("PELEM"));
		// get the root...
		
		
		tags = row.getString("TAGS");
		fileSize = row.getLong("FSIZE");
		checksum = row.getString("CHKSUM");
		// description row.getString("");
		lastModified = row.getLong("MODIFIED");
	}

	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c)
	{
		return null;
	}

	@Override
	public void read(InputStream input) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(ByteListBuffer buffer) {
		// TODO Auto-generated method stub
		
	}

	public String getTags()
	{
		return tags;
	}
}
