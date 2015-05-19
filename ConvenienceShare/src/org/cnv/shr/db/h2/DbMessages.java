package org.cnv.shr.db.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.UserMessage;

public class DbMessages
{
	public static DbIterator<UserMessage> listMessages() throws SQLException
	{
		// sort them!!!
		return new DbIterator<UserMessage>(Services.h2DbCache.getConnection(), DbObjects.MESSAGES);
	}
	
    public static void clearAll() {

		try
		{
			Connection c = Services.h2DbCache.getConnection();
			PreparedStatement stmt = c.prepareStatement("delete from MESSAGE;");
			stmt.execute();
		}
		catch (SQLException ex)
		{
			Services.logger.print(ex);
		}
    }

	public static UserMessage getMessage(int id)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("select * from MESSAGE where M_ID=?;"))
		{
			stmt.setInt(1, id);
			ResultSet results = stmt.executeQuery();
			if (results.next())
			{
				DbObject allocate = DbObjects.MESSAGES.allocate(results);
				allocate.fill(c, results, new DbLocals());
				return (UserMessage) allocate;
			}
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
		
		return null;
	}

	public static void deleteMessage(int id)
	{
		Connection c = Services.h2DbCache.getConnection();
		try (PreparedStatement stmt = c.prepareStatement("delete from MESSAGE where M_ID=?;"))
		{
			stmt.setInt(1, id);
			stmt.execute();
		}
		catch (SQLException e)
		{
			Services.logger.print(e);
		}
	}
}
