package org.cnv.shr.db.h2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.db.mdl.IgnorePattern;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SecurityKey;
import org.cnv.shr.mdl.UserMessage;

public class DbTables
{
	public enum DbObjects
	{
		PUBLIC_KEY       ("PUBLIC_KEY         ".trim(), "K_ID"),
		PELEM            ("PELEM              ".trim(), "P_ID"),
		IGNORE_PATTERN   ("IGNORE_PATTERN     ".trim(), "I_ID"),
		PENDING_DOWNLOAD ("PENDING_DOWNLOAD   ".trim(), "Q_ID"),
		MESSAGES         ("MESSAGES           ".trim(), "M_ID"), 
		LROOT            ("ROOT               ".trim(), "R_ID"),
		RROOT            ("ROOT               ".trim(), "R_ID"),
		LFILE            ("SFILE              ".trim(), "F_ID"),
		RFILE            ("SFILE              ".trim(), "F_ID"),
		LMACHINE         ("MACHINE            ".trim(), "M_ID"),
		RMACHINE         ("MACHINE            ".trim(), "M_ID"),
		ROOT_CONTAINS    ("ROOT_CONTAINS      ".trim(), ""),
		;
		String tableName;
		String pKey;
		
		DbObjects(String name, String pKey)
		{
			this.tableName = name;
			this.pKey = pKey;
		}
		
		public void delete(Connection c) throws SQLException
		{
			try (PreparedStatement stmt = c.prepareStatement("drop table if exists " + getTableName() + ";"))
			{
				stmt.execute();
			}
		}
		
		public void debug(Connection c, PrintStream ps) throws SQLException
		{
//			new Exception().printStackTrace(ps);

			ps.println("Printing " + tableName);
			ps.println("----------------------------------------------");
			ResultSet executeQuery2 = c.prepareStatement("select * from " + tableName + ";").executeQuery();
			int ncols = executeQuery2.getMetaData().getColumnCount();
			for (int i = 1; i < ncols; i++)
			{
				ps.print(executeQuery2.getMetaData().getColumnName(i) + ",");
			}
			ps.println();
			while (executeQuery2.next())
			{
				for (int i = 1; i <= ncols; i++)
				{
					ps.print(executeQuery2.getObject(i) + ",");
				}
				ps.println();
			}
			ps.println("----------------------------------------------");
		}
		
		public DbObject allocate(ResultSet row) throws SQLException
		{
			switch(this)
			{  
				case PUBLIC_KEY       : return new SecurityKey    (row.getInt(pKey));    
				case PELEM            : return new PathElement    (row.getInt(pKey));            
				case IGNORE_PATTERN   : return new IgnorePattern  (row.getInt(pKey));        
				case PENDING_DOWNLOAD : return new Download       (row.getInt(pKey));
				case MESSAGES         : return new UserMessage    (row.getInt(pKey));   
				case LROOT            : return new LocalDirectory (row.getInt(pKey));         
				case RROOT            : return new RemoteDirectory(row.getInt(pKey));   
				case LFILE            : return new LocalFile      (row.getInt(pKey));  
				case RFILE            : return new RemoteFile     (row.getInt(pKey));  
				case RMACHINE         : return new Machine        (row.getInt(pKey));
				case LMACHINE         : return Services.localMachine;
				default:
				return null;
			}
		}
		
		public String getTableName()
		{
			return tableName;
		}
		
		public DbObject find(Connection c, int id, DbLocals locals)
		{
			try (PreparedStatement stmt = c.prepareStatement("select * from " + getTableName() + " where " + pKey + " = " + id + ";"))
			{
				ResultSet executeQuery = stmt.executeQuery();
				DbObject object = allocate(executeQuery);
				object.fill(c, executeQuery, locals);
				return object;
			}
			catch (SQLException ex)
			{
				Services.logger.logStream.println("Unable to create from id " + id + ":" + this);
				ex.printStackTrace(Services.logger.logStream);
				return null;
			}
		}
	}
	

	private static final DbObjects[] ALL_TABLES  = new DbObjects[]
	{
		DbObjects.PUBLIC_KEY      ,
		DbObjects.PELEM           ,
		DbObjects.IGNORE_PATTERN  ,
		DbObjects.PENDING_DOWNLOAD,
		DbObjects.MESSAGES        , 
		DbObjects.LROOT           ,
		DbObjects.LFILE           ,
		DbObjects.LMACHINE        ,
	};

	public static void debugDb(PrintStream ps)
	{
		for (DbObjects table : ALL_TABLES)
		{
			try
			{
				table.debug(Services.h2DbCache.getConnection(), ps);
			}
			catch (SQLException e)
			{
				Services.logger.logStream.println("Unable to debug table " + table + ".");
				e.printStackTrace(Services.logger.logStream);
			}
		}
	}

	public static void deleteDb(Connection c) throws SQLException
	{
		for (DbObjects table : ALL_TABLES)
		{
			try
			{
				table.delete(c);
			}
			catch (SQLException e)
			{
				Services.logger.logStream.println("Unable to delete table " + table + ".");
				e.printStackTrace(Services.logger.logStream);
			}
		}

		// addMachine(Services.localMachine);
		// Services.locals.share(Services.settings.downloadsDirectory.get());
	}
	

	static void createDb(Connection c) throws SQLException, IOException
	{
		execute(c, "/create_h2.sql");
		c.prepareStatement("INSERT INTO PELEM VALUES (0, 0, FALSE, '          ');").execute();
		c.prepareStatement("ALTER TABLE PELEM ADD FOREIGN KEY (PARENT) REFERENCES PELEM(P_ID);").execute();
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