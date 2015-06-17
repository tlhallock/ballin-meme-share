
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.db.h2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DbKeys
{
	private static final QueryWrapper SELECT3 = new QueryWrapper("select K_ID from PUBLIC_KEY where MID=? and KEYSTR=?;");
	private static final QueryWrapper SELECT2 = new QueryWrapper("select KEYSTR from PUBLIC_KEY where MID=? order by ADDED desc limit 1;");
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from PUBLIC_KEY where MID=? and KEYSTR=?;");
	private static final QueryWrapper MERGE1  = new QueryWrapper("merge into PUBLIC_KEY key(MID, KEYSTR) values (DEFAULT, ?, ?, ?, ?);");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select KEYSTR, ADDED from PUBLIC_KEY where MID=?;");
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to append key to byte list buffer.", e);
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
			LogWrapper.getLogger().log(Level.INFO, "Unable to read public key from string", e);
			return null;
		}
	}
	
	public static PublicKey[] getKeys(Machine machine)
	{
		LinkedList<PublicKey> returnValue = new LinkedList<>();
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1))
		{

			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());

			try (ResultSet results = stmt.executeQuery();)
			{
				while (results.next())
				{
					PublicKey pKey = getKey(results.getString(1));
					if (pKey != null)
					{
						returnValue.add(pKey);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get machine keys " + machine, e);
		}
		
		return returnValue.toArray(dummy);
	}
	
	public static void addKey(Machine machine, PublicKey key)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(MERGE1, Statement.RETURN_GENERATED_KEYS))
		{
			String keyStr = getKeyString(key);
			LogWrapper.getLogger().info("Adding key to " + machine.getName() + ": " + keyStr);
			
			int ndx = 1;
			stmt.setString(ndx++, keyStr);
			stmt.setLong(ndx++, System.currentTimeMillis());
			stmt.setLong(ndx++, System.currentTimeMillis());
			stmt.setInt(ndx++, machine.getId());

			stmt.executeUpdate();
			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					int keyId = generatedKeys.getInt(1);
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to add key to " + machine, e);
		}
	}

	public static void removeKey(Machine machine, PublicKey revoke)
	{
		String keyStr = getKeyString(revoke);

		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1))
		{
			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());
			stmt.setString(ndx++, keyStr);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove key from " + machine, e);
		}
	}

	public static PublicKey getKey(Machine m)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT2))
		{
			int ndx = 1;
			stmt.setInt(ndx++, m.getId());
			try (ResultSet results = stmt.executeQuery();)
			{
				if (results.next())
				{
					return getKey(results.getString(1));
				}
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get primary key from " + m, e);
		}
		return null;
	}

	public static boolean machineHasKey(Machine machine, PublicKey oldKey)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT3))
		{
			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());
			stmt.setString(ndx++, getKeyString(oldKey));
			try (ResultSet executeQuery = stmt.executeQuery();)
			{
				return executeQuery.next();
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to see if machine has key " + machine, e);
			return false;
		}
	}
}
