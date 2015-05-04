package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class PathElement extends DbObject
{
	int parentId;
	String value;
	
	
	public PathElement(int parent, String value)
	{
		super(null);
		this.parentId = parent;
		this.value = value;
	}


	public PathElement(int int1)
	{
		super(int1);
	}


	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		
	}

	public String getName()
	{
		return value;
	}
	
	public String[] getDbValues()
	{
		return new String[] {value};
	}


	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c)
	{
		return null;
	}
}
