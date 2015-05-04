package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;

public class PathElement extends DbObject
{
	int parentId;
	String value;
	boolean broken;
	
	
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


	public PathElement(String string, boolean b)
	{
		super(null);
		this.value = string;
		this.broken = b;
	}


	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		id = row.getInt("P_ID");
		parentId = row.getInt("PARENT");
		broken = row.getBoolean("BROKEN");
		value = row.getString("PELEM");
	}
	
	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
			PreparedStatement stmt = c.prepareStatement(
					 "merge into PELEM key(PARENT, PELEM) values (DEFAULT, ?, ?, ?);"
					, Statement.RETURN_GENERATED_KEYS);
			int ndx = 1;
			stmt.setInt(ndx++,  parentId);
			stmt.setBoolean(ndx++, broken);
			stmt.setString(ndx++,     value);
			return stmt;
	}

	public String getName()
	{
		return value;
	}
	
	public String[] getDbValues()
	{
		return new String[] {value};
	}

	public String getFullPath()
	{
		return "";
	}
}
