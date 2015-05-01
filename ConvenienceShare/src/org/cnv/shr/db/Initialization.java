package org.cnv.shr.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

import org.cnv.shr.dmn.Services;

public class Initialization
{
	static void clearDb(Connection c, HashSet<String> currentTables) throws SQLException, IOException
	{
		execute(c, "/clear.sql");
	}

	static void createDb(Connection c) throws SQLException, IOException
	{
		execute(c, "/create.sql");
	}
	
	static HashSet<String> getCurrentTables(Connection c) throws SQLException
	{
		HashSet<String> returnValue = new HashSet<>();
		
		try (PreparedStatement stmt = c.prepareStatement(
					 "select name from sqlite_master where type='table';");)
		{
			ResultSet executeQuery = stmt.executeQuery();
			
			while (executeQuery.next())
			{
				returnValue.add(executeQuery.getString("name"));
			}
		}

		return returnValue;
	}
	
	private static void execute(Connection c, String file) throws SQLException, IOException
	{
		for (PreparedStatement stmt : getStatements(c, file))
		{
			stmt.execute();
			stmt.close();
		}
	}
	
	private static PreparedStatement[] getStatements(Connection c, String file) throws SQLException, IOException
	{
		StringBuilder builder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				ClassLoader.getSystemResourceAsStream(Services.settings.getSqlDir() + file))))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				builder.append(line);
			}
		}

		String[] statements = builder.toString().split(";");
		PreparedStatement[] returnValue = new PreparedStatement[statements.length];

		for (int i = 0; i < statements.length; i++)
		{
			returnValue[i] = c.prepareStatement(statements[i] + ";");
		}
		
		return returnValue;
	}
}
