package org.cnv.shr.db.h2;

import java.security.PublicKey;

import org.cnv.shr.mdl.Machine;

public class DbKeys
{
//	public static DbIterator<SecurityKey> getKeys(Machine machine) throws SQLException
//	{
//		Connection c = Services.h2DbCache.getConnection();
//		return null;
//	}
	
	public static PublicKey[] getKeys(Machine machine)
	{
		return null;
	}
	
	public static void addKey(Machine machine, PublicKey key)
	{
		
	}

	public static void removeKey(Machine machine, PublicKey revoke)
	{
		
	}

	public static PublicKey getKey(Machine m)
	{
		// TODO Auto-generated method stub
		return null;
	}
}
