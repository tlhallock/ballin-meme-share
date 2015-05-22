package org.cnv.shr.mdl;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class IgnorePattern extends DbObject<Integer>
{
	private static final QueryWrapper STRING = new QueryWrapper("");

	public IgnorePattern(int int1)
	{
		super(int1);
	}

	@Override
	public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
	{
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		try (StatementWrapper stmt = c.prepareStatement(STRING);)
		{
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
}
