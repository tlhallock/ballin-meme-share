
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public abstract class DbObject<T>
{
	protected T id;
	
	public DbObject(T id)
	{
		this.id = id;
	}
	
	public abstract void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException;
	
	public void setId(int i)
	{
		throw new RuntimeException("Fix this.");
	}
	
	public T getId()
	{
		return id;
	}

	public final boolean tryToSave()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
		{
			return save(c);
		}
		catch (SQLException e)
		{
				LogWrapper.getLogger().log(Level.INFO, "Unable to save object of type " + getClass().getName(), e);
				return false;
		}
	}

	public abstract boolean save(ConnectionWrapper c) throws SQLException;
}
