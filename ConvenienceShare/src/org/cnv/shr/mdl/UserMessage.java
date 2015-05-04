package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class UserMessage extends DbObject
{
	public UserMessage(int int1)
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getTableName()
	{
		return "MESSAGES";
	}
}
