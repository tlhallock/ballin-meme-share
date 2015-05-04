package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.util.HashMap;

public class DbLocals 
{
	private HashMap<String, Object> cache = new HashMap<>();
	
	public Object getObject(DbTables.DbObjects type, int id)
	{
		return cache.get(getLocalsCache(type, id));
	}
	
	public DbLocals setObject(DbTables.DbObjects type, int id, Object o)
	{
		cache.put(getLocalsCache(type, id), o);
		return this;
	}
	
	public DbObject getObject(Connection c, DbTables.DbObjects type, int id)
	{
		String key = getLocalsCache(type, id);
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
	
	private static String getLocalsCache(DbTables.DbObjects type, int id)
	{
		return type.name() + ":" + id;
	}
}
