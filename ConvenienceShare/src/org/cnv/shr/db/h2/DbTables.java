package org.cnv.shr.db.h2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.IgnorePattern;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.Machine.LocalMachine;
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
		PENDING_DOWNLOAD ("DOWNLOAD           ".trim(), "Q_ID"),
		MESSAGES         ("MESSAGE            ".trim(), "M_ID"), 
		LROOT            ("ROOT               ".trim(), "R_ID"),
		RROOT            ("ROOT               ".trim(), "R_ID"),
		LFILE            ("SFILE              ".trim(), "F_ID"),
		RFILE            ("SFILE              ".trim(), "F_ID"),
		LMACHINE         ("MACHINE            ".trim(), "M_ID"),
		RMACHINE         ("MACHINE            ".trim(), "M_ID"),
		ROOT_CONTAINS    ("ROOT_CONTAINS      ".trim(), ""),
		SHARE_ROOT       ("SHARE_ROOT         ".trim(), ""),
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
			Services.logger.println("Deleting " + getTableName());
			try (PreparedStatement stmt = c.prepareStatement("drop table if exists " + getTableName() + ";"))
			{
				stmt.execute();
			}
		}
		
		public void debug(Connection c)
		{
			try
			{
				StringBuilder builder = new StringBuilder();
				
				builder.append("Printing " + tableName).append('\n');
				Services.logger.println(builder.toString()); builder.setLength(0);
				builder.append("----------------------------------------------").append('\n');
				Services.logger.println(builder.toString()); builder.setLength(0);
				ResultSet executeQuery2 = c.prepareStatement("select * from " + tableName + ";").executeQuery();
				int ncols = executeQuery2.getMetaData().getColumnCount();
				for (int i = 1; i <= ncols; i++)
				{
					builder.append(executeQuery2.getMetaData().getColumnName(i)).append(",");
				}
				builder.append('\n');
				Services.logger.println(builder.toString()); builder.setLength(0);
				
				while (executeQuery2.next())
				{
					for (int i = 1; i <= ncols; i++)
					{
						builder.append(executeQuery2.getObject(i)).append(",");
					}
					builder.append('\n');
					Services.logger.println(builder.toString()); builder.setLength(0);
				}
				builder.append("----------------------------------------------").append('\n');
				Services.logger.println(builder.toString()); builder.setLength(0);
			}
			catch (SQLException ex)
			{
				Services.logger.print(ex);
			}
		}

		public void list()
		{
			
		}
		
		public DbObject allocate(ResultSet row) throws SQLException
		{
			switch(this)
			{  
				case PUBLIC_KEY       : return new SecurityKey    (row.getInt(pKey));    
				case PELEM            : return new PathElement    (row.getLong(pKey));            
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
		
		public static DbObjects get(DbObject object)
		{
			if (object instanceof SecurityKey    ) return PUBLIC_KEY      ;   
			if (object instanceof PathElement    ) return PELEM           ;           
			if (object instanceof IgnorePattern  ) return IGNORE_PATTERN  ;       
			if (object instanceof Download       ) return PENDING_DOWNLOAD;
			if (object instanceof UserMessage    ) return MESSAGES        ;  
			if (object instanceof LocalDirectory ) return LROOT           ;        
			if (object instanceof RemoteDirectory) return RROOT           ;  
			if (object instanceof LocalFile      ) return LFILE           ; 
			if (object instanceof RemoteFile     ) return RFILE           ; 
			if (object instanceof LocalMachine   ) return LMACHINE        ;
			if (object instanceof Machine        ) return RMACHINE        ;
			return null;
		}
		
		public String getTableName()
		{
			return tableName;
		}
		
		public DbObject find(Connection c, int id, DbLocals locals)
		{
			switch (this)
			{
			case RMACHINE:
			case LMACHINE:
				if (id == Services.localMachine.getId())
				{
					return Services.localMachine;
				}
				break;
			case PELEM:
				if (id == DbPaths.ROOT.getId())
				{
					return DbPaths.ROOT;
				}
				break;
			default:
			}
			try (PreparedStatement stmt = c.prepareStatement("select * from " + getTableName() + " where " + pKey + " = ?;"))
			{
				stmt.setInt(1,  id);
				ResultSet executeQuery = stmt.executeQuery();
				if (!executeQuery.next())
				{
					return null;
				}
				DbObject object = allocate(executeQuery);
				object.fill(c, executeQuery, locals);
				return object;
			}
			catch (SQLException ex)
			{
				Services.logger.println("Unable to create from id " + id + ":" + this);
				Services.logger.print(ex);
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
		DbObjects.ROOT_CONTAINS   ,
		DbObjects.SHARE_ROOT      ,
	};

	public static void debugDb()
	{
		for (DbObjects table : ALL_TABLES)
		{
			table.debug(Services.h2DbCache.getConnection());
		}
	}

	public static void deleteDb(Connection c)
	{
		for (DbObjects table : ALL_TABLES)
		{
			try
			{
				table.delete(c);
			}
			catch (SQLException e)
			{
				Services.logger.println("Unable to delete table " + table + ".");
				Services.logger.print(e);
			}
		}

		// addMachine(Services.localMachine);
		// Services.locals.share(Services.settings.downloadsDirectory.get());
	}
	

	static void createDb(Connection c) throws SQLException, IOException
	{
		executeStatments(c, "/create_h2.sql");
	}
	
	private static PreparedStatement[] executeStatments(Connection c, String file) throws SQLException, IOException
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
			try (PreparedStatement stmt = c.prepareStatement(statements[i] + ";");)
			{
				System.out.println("Executing " + statements[i]);
				stmt.execute();
			}
		}

		return returnValue;
	}
}