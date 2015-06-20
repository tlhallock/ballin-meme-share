
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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbChunks.DbChunk;
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
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

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
		CHUNK            ("CHUNK              ".trim(), "C_ID"),
		ROOT_CONTAINS    ("ROOT_CONTAINS      ".trim(), ""),
		SHARE_ROOT       ("SHARE_ROOT         ".trim(), ""),
		CHKSUM_REQ       ("CHK_REQ            ".trim(), ""),
		;
		String tableName;
		String pKey;
		
		DbObjects(String name, String pKey)
		{
			this.tableName = name;
			this.pKey = pKey;
		}
		
		public void delete(ConnectionWrapper c) throws SQLException
		{
			LogWrapper.getLogger().info("Deleting " + getTableName());
			try (StatementWrapper stmt = c.prepareNewStatement("drop table if exists " + getTableName() + ";"))
			{
				stmt.execute();
			}
		}
		
		public void debug(ConnectionWrapper c)
		{
			try
			{
				StringBuilder builder = new StringBuilder();
				
				builder.append("Printing " + tableName).append('\n');
				LogWrapper.getLogger().info(builder.toString()); builder.setLength(0);
				builder.append("----------------------------------------------").append('\n');
				LogWrapper.getLogger().info(builder.toString()); builder.setLength(0);
				try (ResultSet executeQuery2 = c.prepareNewStatement("select * from " + tableName + ";").executeQuery();)
				{
					int ncols = executeQuery2.getMetaData().getColumnCount();
					for (int i = 1; i <= ncols; i++)
					{
						builder.append(executeQuery2.getMetaData().getColumnName(i)).append(",");
					}
					builder.append('\n');
					LogWrapper.getLogger().info(builder.toString());
					builder.setLength(0);

					while (executeQuery2.next())
					{
						for (int i = 1; i <= ncols; i++)
						{
							builder.append(executeQuery2.getObject(i)).append(",");
						}
						builder.append('\n');
						LogWrapper.getLogger().info(builder.toString());
						builder.setLength(0);
					}
					builder.append("----------------------------------------------").append('\n');
					LogWrapper.getLogger().info(builder.toString());
					builder.setLength(0);
				}
			}
			catch (SQLException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to debug", ex);
			}
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
				case CHUNK            : return new DbChunk        (row.getInt(pKey));
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
			if (object instanceof DbChunk        ) return CHUNK           ;
			return null;
		}
		
		public String getTableName()
		{
			return tableName;
		}
		
		public DbObject find(ConnectionWrapper c, int id, DbLocals locals)
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
			try (StatementWrapper stmt = c.prepareNewStatement("select * from " + getTableName() + " where " + pKey + " = ?;"))
			{
				stmt.setInt(1, id);
				try (ResultSet executeQuery = stmt.executeQuery();)
				{
					if (!executeQuery.next())
					{
						return null;
					}
					DbObject object = allocate(executeQuery);
					object.fill(c, executeQuery, locals);
					return object;
				}
			}
			catch (SQLException ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to create from id " + id + ":" + this, ex);
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
		DbObjects.CHUNK           ,
		DbObjects.CHKSUM_REQ      ,
	};

	public static void debugDb()
	{
		try (ConnectionWrapper c = Services.h2DbCache.getThreadConnection();)
		{
			for (DbObjects table : ALL_TABLES)
			{
				table.debug(c);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static void deleteDb(ConnectionWrapper c)
	{
		try
		{
			for (DbObjects table : ALL_TABLES)
			{
				table.delete(c);
			}
			createDb(c);
			if (Services.localMachine != null)
				Services.localMachine.save(c);
		}
		catch (SQLException |IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to delete database:", e);
		}

		// addMachine(Services.localMachine);
		// Services.locals.share(Services.settings.downloadsDirectory.get());
	}
	

	static void createDb(ConnectionWrapper c) throws SQLException, IOException
	{
		executeStatments(c, "create_h2.sql");
	}
	
	private static PreparedStatement[] executeStatments(ConnectionWrapper c, String file) throws SQLException, IOException
	{
		String[] statements = Misc.readFile(Settings.RES_DIR + file).split(";");
		PreparedStatement[] returnValue = new PreparedStatement[statements.length];

		for (int i = 0; i < statements.length; i++)
		{
			try (StatementWrapper stmt = c.prepareNewStatement(statements[i] + ";");)
			{
				LogWrapper.getLogger().fine("Executing " + statements[i]);
				stmt.execute();
			}
		}

		return returnValue;
	}
}
