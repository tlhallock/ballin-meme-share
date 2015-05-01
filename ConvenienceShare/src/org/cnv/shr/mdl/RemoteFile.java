package org.cnv.shr.mdl;

import org.cnv.shr.dmn.Services;

public class RemoteFile extends SharedFile
{
	RemoteDirectory d;
	String localCopy;
	
	
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
