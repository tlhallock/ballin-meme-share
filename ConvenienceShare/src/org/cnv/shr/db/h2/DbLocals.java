
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

import java.util.HashMap;

import org.cnv.shr.db.h2.DbTables.DbObjects;

public class DbLocals 
{
	private HashMap<String, DbObject> cache = new HashMap<>();
	
	public DbObject getObject(DbTables.DbObjects type, int id)
	{
		return cache.get(getLocalsCache(type, String.valueOf(id)));
	}
	
	public DbLocals setObject(DbObject o)
	{
		cache.put(getLocalsCache(DbObjects.get(o), String.valueOf(o.getId())), o);
		return this;
	}
	
	public DbObject getObject(ConnectionWrapper c, DbObjects type, int id)
	{
		String key = getLocalsCache(type, String.valueOf(id));
		Object object = cache.get(key);
		if (object != null)
		{
			return (DbObject) object;
		}
		DbObject o = type.find(c, id, this);
		if (o != null)
		{
			cache.put(key, o);
		}
		return o;
	}
	
	private static String getLocalsCache(DbTables.DbObjects type, String id)
	{
		return type.getTableName() + ":" + id;
	}
}
