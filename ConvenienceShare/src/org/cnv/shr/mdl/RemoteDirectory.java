package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RemoteDirectory extends RootDirectory
{
	public RemoteDirectory(Machine machine, String path, String tags, String description)
	{
		super(machine, path, tags, description);
	}

	public RemoteDirectory(int int1)
	{
		super(int1);
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}

	@Override
	public void synchronizeInternal()
	{
		
	}

	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
