package org.cnv.shr.db.h2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class DbKeys
{
	private static final PublicKey[] dummy = new PublicKey[0];
//	private static final String NULLKEY = getNullKey();
	
//	private static String getNullKey()
//	{
//		int keyLength = 280;
//		StringBuilder builder = new StringBuilder(keyLength);
//		for (int i = 0; i < keyLength; i++)
//		{
//			builder.append(' ');
//		}
//		return builder.toString();
//	}
	private static String getKeyString(PublicKey key)
	{
//		if (key == null)
//		{
//			return NULLKEY;
//		}
		ByteListBuffer byteListBuffer = new ByteListBuffer();
		try
		{
			byteListBuffer.append(key);
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
		return Misc.format(byteListBuffer.getBytes()); 
	}
	
	private static PublicKey getKey(String keyString)
	{
//		if (keyString.equals(NULLKEY))
//		{
//			return null;
//		}
		try (InputStream is = new ByteArrayInputStream(Misc.format(keyString));)
		{
			return new ByteReader(is).readPublicKey();
		}
		catch (IOException e)
		{
			Services.logger.print(e);
			return null;
		}
	}
	
	public static PublicKey[] getKeys(Machine machine)
	{
		LinkedList<PublicKey> returnValue = new LinkedList<>();
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select KEYSTR, ADDED from PUBLIC_KEY where MID=?;"))
		{

			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());

			ResultSet results = stmt.executeQuery();
			while (results.next())
			{
				PublicKey pKey = getKey(results.getString(1));
				if (pKey != null)
				{
					returnValue.add(pKey);
				}
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		
		return returnValue.toArray(dummy);
	}
	
	public static void addKey(Machine machine, PublicKey key)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				"merge into PUBLIC_KEY key(MID, KEYSTR) values (DEFAULT, ?, ?, ?, ?);",
				Statement.RETURN_GENERATED_KEYS))
		{
			String keyStr = getKeyString(key);
			Services.logger.println("Adding key to " + machine.getName() + ": " + keyStr);
			
			int ndx = 1;
			stmt.setString(ndx++, keyStr);
			stmt.setLong(ndx++, System.currentTimeMillis());
			stmt.setLong(ndx++, System.currentTimeMillis());
			stmt.setInt(ndx++, machine.getId());

			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				int keyId = generatedKeys.getInt(1);
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}

	public static void removeKey(Machine machine, PublicKey revoke)
	{
		String keyStr = getKeyString(revoke);

		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("delete from PUBLIC_KEY where MID=? and KEYSTR=?;"))
		{
			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());
			stmt.setString(ndx++, keyStr);
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}

	public static PublicKey getKey(Machine m)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement(
				"select KEYSTR from PUBLIC_KEY where MID=? order by ADDED desc limit 1;"))
		{
			int ndx = 1;
			stmt.setInt(ndx++, m.getId());
			ResultSet results = stmt.executeQuery();
			if (results.next())
			{
				return getKey(results.getString(1));
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		return null;
	}

	public static boolean machineHasKey(Machine machine, PublicKey oldKey)
	{
		Connection c = Services.h2DbCache.getConnection();
		DbObjects.PUBLIC_KEY.debug(c);
		try (PreparedStatement stmt = c.prepareStatement(
				"select K_ID from PUBLIC_KEY where MID=? and KEYSTR=?;"))
		{
			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());
			stmt.setString(ndx++, getKeyString(oldKey));
			return stmt.executeQuery().next();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
			return false;
		}
	}
}
