package org.cnv.shr.mdl;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class UserMessage extends DbObject
{
	public UserMessage(Integer int1)
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
		return null;
	}
	
	public static class AuthenticationRequest extends UserMessage
	{
		public AuthenticationRequest(int int1)
		{
			super(int1);
		}

		public AuthenticationRequest(Machine m, PublicKey key)
		{
			super(null);
		}
	}
}
