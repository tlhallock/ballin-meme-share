package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SecurityKey;

public class DbKeys
{
	public static DbIterator<SecurityKey> getKys(Machine machine) throws SQLException
	{
		Connection c = Services.h2DbCache.getConnection();
		return null;
	}
}
