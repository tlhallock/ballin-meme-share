package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbTables;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.db.h2.PathBreaker;
import org.cnv.shr.db.h2.RStringBuilder;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.sync.FileSource;

public class PathElement extends DbObject
{
	protected PathElement parentId;
	protected String value;
	protected boolean broken;
	
	String fullPath;
	
	public PathElement(Integer id)
	{
		super(id);
	}
	
	public PathElement(PathElement parent, String value)
	{
		super(null);
		this.parentId = parent;
		this.value = value;
		
		if (value.length() > PathBreaker.PATH_ELEMENT_LENGTH)
		{
			throw new RuntimeException("Value is too big: " + value);
		}
	}

	public PathElement(PathElement current, int integer, String string)
	{
		super(integer);
		parentId = current;
		id = integer;
		value = string;
		
		if (value.length() > PathBreaker.PATH_ELEMENT_LENGTH)
		{
			throw new RuntimeException("Value is too big: " + value);
		}
	}


	public PathElement(PathElement parent, String string, boolean b)
	{
		super(null);
		this.parentId = parent;
		this.value = string;
		this.broken = b;
	}
	
	public String toString()
	{
		return getFullPath();
	}

	public void setId(int i)
	{
		id = i;
	}

	public PathElement getParent()
	{
		return parentId;
	}


	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		id       = row.getInt("P_ID");
		parentId = (PathElement) locals.getObject(c, DbTables.DbObjects.PELEM, row.getInt("PARENT"));
		broken   = row.getBoolean("BROKEN");
		value    = row.getString("PELEM");
	}
	
	@Override
	protected PreparedStatement createPreparedUpdateStatement(Connection c) throws SQLException
	{
		PreparedStatement stmt = c.prepareStatement(
					 "merge into PELEM key(PARENT, PELEM) values ((select P_ID from PELEM where PARENT=? and PELEM=?), ?, ?, ?);"
					, Statement.RETURN_GENERATED_KEYS);
		int ndx = 1;

		stmt.setInt(ndx++, getParent().getId());
		stmt.setString(ndx++, value);
		stmt.setInt(ndx++, getParent().getId());
		stmt.setBoolean(ndx++, broken);
		stmt.setString(ndx++, value);
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

	public String getUnbrokenName()
	{
		PathElement c = this;
		RStringBuilder builder = new RStringBuilder();
		do
		{
			builder.preppend(c.value);
			c = c.getParent();
		} while (c.isBroken());
		return builder.toString();
	}

	public String getFullPath()
	{
		if (fullPath != null)
		{
			return fullPath;
		}
		PathElement c = this;
		RStringBuilder builder = new RStringBuilder();
		while (c.getParent() != c)
		{
			builder.preppend(c.value);
			c = c.getParent();
		}
		return fullPath = builder.toString();
	}
	
	public LinkedList<PathElement> list(RootDirectory local)
	{
		LinkedList<PathElement> returnValue = new LinkedList<>();
		LinkedList<PathElement> broken = new LinkedList<PathElement>();
		
		DbIterator<PathElement> iterator = DbPaths.listPathElements(local, this);

		// not as simple as the recursive call, but only opens one statement at a time...
		for (;;)
		{
			while (iterator.hasNext())
			{
				PathElement next = iterator.next();
				if (next.isBroken())
				{
					broken.add(next);
				}
				else
				{
					returnValue.add(next);
				}
			}
			
			if (broken.isEmpty())
			{
				return returnValue;
			}
			
			DbObjects.PELEM.debug(Services.h2DbCache.getConnection(), System.out);
			
			iterator = DbPaths.listPathElements(local, broken.removeLast());
		}
	}

	public boolean isBroken()
	{
		return broken;
	}
	
	public static String sanitizeFilename(FileSource f)
	{
		String name = f.getName();
		if (f.isDirectory())
		{
			if (name.charAt(name.length() - 1) != '/')
			{
				name = name + "/";
			}
			return name;
		}
		else if (f.isFile())
		{
			return name;
		}
		
		return null;
	}
	
	public boolean equals(PathElement other)
	{
		return id == other.id;
	}
}
