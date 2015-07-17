package org.cnv.shr.mdl;

public enum RootDirectoryType
{
	LOCAL (1),
	REMOTE(2),
	MIRROR(3),
	
	;
	
	private int dbValue;
	
	RootDirectoryType(int dbValue)
	{
		this.dbValue = dbValue;
	}
	
	public int getDbValue()
	{
		return dbValue;
	}

	public static RootDirectoryType findType(int daValue)
	{
		for (RootDirectoryType type : values())
		{
			if (type.dbValue == daValue)
			{
				return type;
			}
		}
		return null;
	}
}