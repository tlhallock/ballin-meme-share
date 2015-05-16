package org.cnv.shr.db.h2;

import java.sql.Connection;
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
	
	public DbObject getObject(Connection c, DbObjects type, int id)
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
