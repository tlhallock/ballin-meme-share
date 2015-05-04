package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class SecurityKey extends DbObject
{
	Machine machine;
	String value;
	long created;
	long expires;
	
	public SecurityKey(int int1)
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getTableName()
	{
		return "PUBLIC_KEY";
	}
	
	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		// TODO Auto-generated method stub
		
	}
}
