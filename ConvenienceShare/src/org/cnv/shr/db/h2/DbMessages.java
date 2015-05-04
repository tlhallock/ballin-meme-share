package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.SQLException;

import org.cnv.shr.mdl.SecurityKey;
import org.cnv.shr.mdl.UserMessage;

public class DbMessages
{
	static Allocator<SecurityKey> allocator = new Allocator<SecurityKey> ()
	{
		@Override
		public SecurityKey create()
		{
			return new SecurityKey();
		}
	};
	
	public static DbIterator<UserMessage> listMessages(Connection c) throws SQLException
	{
		return null;
	}
}
