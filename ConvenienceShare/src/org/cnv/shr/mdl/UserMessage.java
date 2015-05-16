package org.cnv.shr.mdl;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class UserMessage extends DbObject<Integer>
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
	public boolean save(Connection c) throws SQLException
	{
		try (PreparedStatement stmt = c.prepareStatement("");)
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
