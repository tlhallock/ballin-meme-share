package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.UserMessage;

public class DbMessages
{
	public static DbIterator<UserMessage> listMessages() throws SQLException
	{
		Connection c = Services.h2DbCache.getConnection();
		return null;
	}
}
