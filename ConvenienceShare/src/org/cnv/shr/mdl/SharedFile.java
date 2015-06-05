package org.cnv.shr.mdl;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.trck.FileEntry;

public abstract class SharedFile extends DbObject<Integer>
{
	private static final QueryWrapper MERGE1 = new QueryWrapper("merge into SFILE key(PELEM, ROOT) values ((select F_ID from SFILE where PELEM=? and ROOT=?), ?, ?, ?, ?, ?, ?, ?, ?);");
	
	
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

	public void setChecksum(String checksum) throws IOException
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
	public String toString()
	{
		return rootDirectory + ":" + path;
	}

	@Override
	public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
	{
		id = row.getInt("F_ID");

		int rootId = row.getInt("ROOT");
		
		rootDirectory =  fillRoot(c, locals, rootId);
		path          =    (PathElement) locals.getObject(c, DbTables.DbObjects.PELEM, row.getInt("PELEM"));
		
		tags = row.getString("TAGS");
		fileSize = row.getLong("FSIZE");
		checksum = row.getString("CHKSUM");
		lastModified = row.getLong("MODIFIED");
	}

	protected abstract RootDirectory fillRoot(ConnectionWrapper c, DbLocals locals, int rootId);

	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		try (StatementWrapper stmt = c.prepareStatement(MERGE1, Statement.RETURN_GENERATED_KEYS);)
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
			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
					return true;
				}
				// maybe no key was generated...
				return true;
			}
		}
	}

	public String getTags()
	{
		return tags;
	}

	public abstract boolean isLocal();
	
	public FileEntry getFileEntry()
	{
		return new FileEntry(getChecksum(), fileSize);
	}
}
