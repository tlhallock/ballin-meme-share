package org.cnv.shr.mdl;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public class RemoteFile extends SharedFile
{
	RemoteDirectory d;
	String localCopy;

	public RemoteFile(int int1)
	{
		super(int1);
	}

	public RemoteFile(RemoteDirectory root, PathElement pathElement,
			long fileSize, String checksum, String tags, long lastModified)
	{
		super(null);
		rootDirectory = root;
		path = pathElement;
		this.fileSize = fileSize;
		this.checksum = checksum;
		this.tags = tags;
		this.lastModified = lastModified;
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
		
		SharedFileState getState(int dbValue)
		{
			for (SharedFileState s : SharedFileState.values())
			{
				if (s.dbValue == dbValue)
				{
					return s;
				}
			}
			Services.logger.logStream.println("Uknown file state: " + dbValue);
			return null;
		}
	}
}
