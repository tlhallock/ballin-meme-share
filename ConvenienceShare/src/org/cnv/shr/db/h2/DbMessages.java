package org.cnv.shr.db.h2;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.UserMessage;

public class DbMessages
{
	private static final QueryWrapper DELETE2 = new QueryWrapper("delete from MESSAGE where M_ID=?;");
	private static final QueryWrapper DELETE1 = new QueryWrapper("delete from MESSAGE;");
	private static final QueryWrapper SELECT1 = new QueryWrapper("select * from MESSAGE where M_ID=?;");

	public static DbIterator<UserMessage> listMessages() throws SQLException
	{
		// sort them!!!
		return new DbIterator<UserMessage>(Services.h2DbCache.getThreadConnection(), DbObjects.MESSAGES);
	}
	
    public static void clearAll() {

		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE1);)
		{
			stmt.execute();
		}
		catch (SQLException ex)
		{
			Services.logger.print(ex);
		}
    }

	public static UserMessage getMessage(int id)
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(SELECT1))
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
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = c.prepareStatement(DELETE2))
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
