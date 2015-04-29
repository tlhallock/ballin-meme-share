package org.cnv.shr.db;

import java.sql.Connection;
import java.sql.DriverManager;

import org.cnv.shr.dmn.Services;

public class DbConnection
{
	
	
	public static void initialize() throws Exception
	{
		Class.forName("org.sqlite.JDBC");
		Connection c = DriverManager.getConnection("jdbc:sqlite:" 
				+ Services.settings.applicationDirectory + "/files.db");
		
		
	}

}
