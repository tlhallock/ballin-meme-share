package org.cnv.shr.mdl;

import java.io.InputStream;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.ByteListBuffer;

public class RemoteFile extends SharedFile
{
	RemoteDirectory d;
	String localCopy;

	public RemoteFile(int int1)
	{
		super(int1);
	}

	public RemoteFile(Machine machine, RemoteDirectory remote, InputStream input)
	{
		super(null);
		
		
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
