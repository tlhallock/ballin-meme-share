package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
		super(int1);
	}
	
	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
