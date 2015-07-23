
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

package org.cnv.shr.db.h2.bak;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbChunks;
import org.cnv.shr.db.h2.DbChunks.DbChunk;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.json.JsonList;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;


public class DownloadBackup implements Jsonable
{
	// File info
	private String remoteMachine;
	private String remoteDirectory;
	private String remotePath;
	private String checksum;
	private long fileSize;
	
	@MyParserNullable
	private String tags;
	private long lastModified;
	
	// Download info
	private String currentDownloadState;
	private long added;
	private int priority;
	private long chunkSize;
	
	private JsonList<Chunk> chunks = new JsonList<>(new JsonList.Allocator<Chunk>()
	{
		@Override
		public Chunk create(JsonParser parser)
		{
			return new Chunk(parser);
		}
	});
	
	public DownloadBackup(Download download)
	{
		RemoteFile file = download.getFile();
		
		remoteMachine = file.getRootDirectory().getMachine().getIdentifier();
		remoteDirectory = file.getRootDirectory().getName();
		remotePath = file.getPath().getFullPath();
		checksum = file.getChecksum();
		fileSize = file.getFileSize();
		tags = file.getTags();
		lastModified = file.getLastUpdated();
		currentDownloadState = download.getState().name();
		added = download.getAdded();
		priority = download.getPriority();
		chunkSize = download.getChunkSize();
		
		try (DbIterator<DbChunk> iterator = DbChunks.getAllChunks(download))
		{
			while (iterator.hasNext())
			{
				chunks.add(iterator.next().chunk);
			}
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().info("Unable to get chunks of " + download);
		}
	}
	
	
	public void save(ConnectionWrapper wrapper) throws IOException
	{
		Machine remote = DbMachines.getMachine(remoteMachine);
		if (remote == null)
		{
			LogWrapper.getLogger().info("Unable to get remote machine, it should have been saved in the json: " + remoteMachine);
			return;
		}
		RemoteDirectory root = (RemoteDirectory) DbRoots.getRoot(remote, remoteDirectory);
		if (root == null)
		{
			// The tags and description will be updated when the user synchronizes...
			root = new RemoteDirectory(remote, remoteDirectory, null, null, SharingState.DO_NOT_SHARE);
			try
			{
				root.save(wrapper);
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to save remote directory " + remoteDirectory, e);
				return;
			}
		}
		PathElement pathElement = DbPaths2.addFilePath(root, remotePath);
		RemoteFile remoteFile = new RemoteFile(root, pathElement, fileSize, checksum, tags, lastModified);
		try
		{
			remoteFile.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save remote file " + this, e);
			return;
		}

		DownloadState state = DownloadState.valueOf(currentDownloadState);
		if (state == null)
		{
			LogWrapper.getLogger().info("Unkown download state: " + currentDownloadState);
			return;
		}
		Download download = new Download(remoteFile, added, priority, chunkSize, state);
		
		try
		{
			download.save(wrapper);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save download " + this, e);
		}
		
		for (Chunk c : chunks)
		{
			DbChunks.addChunk(download, c);
		}
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("remoteMachine", remoteMachine);
		generator.write("remoteDirectory", remoteDirectory);
		generator.write("remotePath", remotePath);
		generator.write("checksum", checksum);
		generator.write("fileSize", fileSize);
		if (tags!=null)
		generator.write("tags", tags);
		generator.write("lastModified", lastModified);
		generator.write("currentDownloadState", currentDownloadState);
		generator.write("added", added);
		generator.write("priority", priority);
		generator.write("chunkSize", chunkSize);
		{
			generator.writeStartArray("chunks");
			chunks.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsFileSize = true;
		boolean needsLastModified = true;
		boolean needsAdded = true;
		boolean needsPriority = true;
		boolean needsChunkSize = true;
		boolean needsRemoteMachine = true;
		boolean needsRemoteDirectory = true;
		boolean needsRemotePath = true;
		boolean needsChecksum = true;
		boolean needsCurrentDownloadState = true;
		boolean needsChunks = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsFileSize)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"fileSize\"");
				}
				if (needsLastModified)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"lastModified\"");
				}
				if (needsAdded)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"added\"");
				}
				if (needsPriority)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"priority\"");
				}
				if (needsChunkSize)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"chunkSize\"");
				}
				if (needsRemoteMachine)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"remoteMachine\"");
				}
				if (needsRemoteDirectory)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"remoteDirectory\"");
				}
				if (needsRemotePath)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"remotePath\"");
				}
				if (needsChecksum)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"checksum\"");
				}
				if (needsCurrentDownloadState)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"currentDownloadState\"");
				}
				if (needsChunks)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.db.h2.bak.DownloadBackup\" needs \"chunks\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "fileSize":
					needsFileSize = false;
					fileSize = Long.parseLong(parser.getString());
					break;
				case "lastModified":
					needsLastModified = false;
					lastModified = Long.parseLong(parser.getString());
					break;
				case "added":
					needsAdded = false;
					added = Long.parseLong(parser.getString());
					break;
				case "priority":
					needsPriority = false;
					priority = Integer.parseInt(parser.getString());
					break;
				case "chunkSize":
					needsChunkSize = false;
					chunkSize = Long.parseLong(parser.getString());
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "remoteMachine":
					needsRemoteMachine = false;
					remoteMachine = parser.getString();
					break;
				case "remoteDirectory":
					needsRemoteDirectory = false;
					remoteDirectory = parser.getString();
					break;
				case "remotePath":
					needsRemotePath = false;
					remotePath = parser.getString();
					break;
				case "checksum":
					needsChecksum = false;
					checksum = parser.getString();
					break;
				case "tags":
					tags = parser.getString();
					break;
				case "currentDownloadState":
					needsCurrentDownloadState = false;
					currentDownloadState = parser.getString();
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			case START_ARRAY:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("chunks")) {
					needsChunks = false;
					chunks.parse(parser);
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "DownloadBackup"; }
	public String getJsonKey() { return getJsonName(); }
	public DownloadBackup(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
