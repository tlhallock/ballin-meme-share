package org.cnv.shr.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public abstract class SharedFileIterator implements Iterator<SharedFile>
{
	private RootDirectory dir;
	private ResultSet results;
	private SharedFile next;
	
	protected SharedFileIterator(RootDirectory dir, ResultSet results) throws SQLException
	{
		this.dir = dir;
		this.results = results;
		next = findNext();
	}
	
	private SharedFile findNext()
	{
		try
		{
			if (!results.next())
			{
				return null;
			}
				
			SharedFile returnValue = create();
		   // db has root
		   // db has state
			   
			returnValue.setId(                         results.getInt   (1));
			returnValue.setName(                       results.getString(2));
			returnValue.setFileSize(                   results.getLong  (3));
			returnValue.setChecksum(                   results.getString(4));
			returnValue.setPath(Services.db.getPath(   results.getInt   (5)));
			returnValue.setDescription(                "");
			returnValue.setLastUpdated(                results.getLong  (8));
			returnValue.setRootDirectory(              dir);
			
			return returnValue;
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to create a shared file from a record");
			e.printStackTrace(Services.logger.logStream);
			return null;
		}
	}
	
	protected abstract SharedFile create();

	@Override
	public boolean hasNext()
	{
		return next != null;
	}

	@Override
	public SharedFile next()
	{
		SharedFile returnValue = next;
		next = findNext();
		return returnValue;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("Unable to delete.");
	}
	
	public static class LocalFileIterator extends SharedFileIterator
	{
		LocalFileIterator(LocalDirectory root, ResultSet set) throws SQLException
		{
			super(root, set);
		}
		@Override
		protected SharedFile create()
		{
			return new LocalFile();
		}
	}
	public static class RemoteFileIterator extends SharedFileIterator
	{
		RemoteFileIterator(RemoteDirectory root, ResultSet set) throws SQLException
		{
			super(root, set);
		}

		@Override
		protected SharedFile create()
		{
			return new RemoteFile();
		}
	}
}