
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


package org.cnv.shr.json;

import java.util.HashMap;

import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.util.Jsonable;

public class JsonAllocators
{
	private interface JsonAllocator<T extends Jsonable>
	{
		T create(JsonParser p);
	}
	
	
	public static Jsonable create(String className, JsonParser p)
	{
		JsonAllocator<?> jsonAllocator = ALLOCATORS.get(className);
		if (jsonAllocator == null)
		{
			return null;
		}
		return jsonAllocator.create(p);
	}
	
	
	private static final HashMap<String, JsonAllocator<?>> ALLOCATORS = new HashMap<>();
	
	
	static
	{                                                                      
		ALLOCATORS.put(SharingState.getJsonName(), new JsonAllocator<org.cnv.shr.db.h2.SharingState>()          
		{                                                                     
			public org.cnv.shr.db.h2.SharingState create(JsonParser p) { return SharingState.valueOf(p.getString()); }  
		});
		
		ALLOCATORS.put(org.cnv.shr.trck.TrackerEntry.getJsonName(), new JsonAllocator<org.cnv.shr.trck.TrackerEntry>()          
				{                                                                     
					public org.cnv.shr.trck.TrackerEntry create(JsonParser p) { return new org.cnv.shr.trck.TrackerEntry(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.trck.MachineEntry.getJsonName(), new JsonAllocator<org.cnv.shr.trck.MachineEntry>()          
				{                                                                     
					public org.cnv.shr.trck.MachineEntry create(JsonParser p) { return new org.cnv.shr.trck.MachineEntry(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.trck.CommentEntry.getJsonName(), new JsonAllocator<org.cnv.shr.trck.CommentEntry>()          
				{                                                                     
					public org.cnv.shr.trck.CommentEntry create(JsonParser p) { return new org.cnv.shr.trck.CommentEntry(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.trck.FileEntry.getJsonName(), new JsonAllocator<org.cnv.shr.trck.FileEntry>()          
				{                                                                     
					public org.cnv.shr.trck.FileEntry create(JsonParser p) { return new org.cnv.shr.trck.FileEntry(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.trck.Done.getJsonName(), new JsonAllocator<org.cnv.shr.trck.Done>()          
				{                                                                     
					public org.cnv.shr.trck.Done create(JsonParser p) { return new org.cnv.shr.trck.Done(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.dmn.dwn.Chunk.getJsonName(), new JsonAllocator<org.cnv.shr.dmn.dwn.Chunk>()          
				{                                                                     
					public org.cnv.shr.dmn.dwn.Chunk create(JsonParser p) { return new org.cnv.shr.dmn.dwn.Chunk(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.dmn.dwn.SharedFileId.getJsonName(), new JsonAllocator<org.cnv.shr.dmn.dwn.SharedFileId>()          
				{                                                                     
					public org.cnv.shr.dmn.dwn.SharedFileId create(JsonParser p) { return new org.cnv.shr.dmn.dwn.SharedFileId(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.PathListChild.getJsonName(), new JsonAllocator<org.cnv.shr.msg.PathListChild>()          
				{                                                                     
					public org.cnv.shr.msg.PathListChild create(JsonParser p) { return new org.cnv.shr.msg.PathListChild(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.RootListChild.getJsonName(), new JsonAllocator<org.cnv.shr.msg.RootListChild>()          
				{                                                                     
					public org.cnv.shr.msg.RootListChild create(JsonParser p) { return new org.cnv.shr.msg.RootListChild(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.DownloadFailure.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.DownloadFailure>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.DownloadFailure create(JsonParser p) { return new org.cnv.shr.msg.dwn.DownloadFailure(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.ChunkList.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.ChunkList>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.ChunkList create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChunkList(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.DownloadDone.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.DownloadDone>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.DownloadDone create(JsonParser p) { return new org.cnv.shr.msg.dwn.DownloadDone(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.CompletionStatus.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.CompletionStatus>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.CompletionStatus create(JsonParser p) { return new org.cnv.shr.msg.dwn.CompletionStatus(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.FileRequest.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.FileRequest>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.FileRequest create(JsonParser p) { return new org.cnv.shr.msg.dwn.FileRequest(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.ChunkResponse.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.ChunkResponse>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.ChunkResponse create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChunkResponse(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.RequestCompletionStatus.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.RequestCompletionStatus>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.RequestCompletionStatus create(JsonParser p) { return new org.cnv.shr.msg.dwn.RequestCompletionStatus(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.ChunkRequest.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.ChunkRequest>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.ChunkRequest create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChunkRequest(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.MachineHasFile.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.MachineHasFile>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.MachineHasFile create(JsonParser p) { return new org.cnv.shr.msg.dwn.MachineHasFile(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.ListRoots.getJsonName(), new JsonAllocator<org.cnv.shr.msg.ListRoots>()          
				{                                                                     
					public org.cnv.shr.msg.ListRoots create(JsonParser p) { return new org.cnv.shr.msg.ListRoots(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.ListPath.getJsonName(), new JsonAllocator<org.cnv.shr.msg.ListPath>()          
				{                                                                     
					public org.cnv.shr.msg.ListPath create(JsonParser p) { return new org.cnv.shr.msg.ListPath(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.MachineFound.getJsonName(), new JsonAllocator<org.cnv.shr.msg.MachineFound>()          
				{                                                                     
					public org.cnv.shr.msg.MachineFound create(JsonParser p) { return new org.cnv.shr.msg.MachineFound(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.DoneMessage.getJsonName(), new JsonAllocator<org.cnv.shr.msg.DoneMessage>()          
				{                                                                     
					public org.cnv.shr.msg.DoneMessage create(JsonParser p) { return new org.cnv.shr.msg.DoneMessage(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.PathList.getJsonName(), new JsonAllocator<org.cnv.shr.msg.PathList>()          
				{                                                                     
					public org.cnv.shr.msg.PathList create(JsonParser p) { return new org.cnv.shr.msg.PathList(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.RootList.getJsonName(), new JsonAllocator<org.cnv.shr.msg.RootList>()          
				{                                                                     
					public org.cnv.shr.msg.RootList create(JsonParser p) { return new org.cnv.shr.msg.RootList(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.FindMachines.getJsonName(), new JsonAllocator<org.cnv.shr.msg.FindMachines>()          
				{                                                                     
					public org.cnv.shr.msg.FindMachines create(JsonParser p) { return new org.cnv.shr.msg.FindMachines(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.UserMessageMessage.getJsonName(), new JsonAllocator<org.cnv.shr.msg.UserMessageMessage>()          
				{                                                                     
					public org.cnv.shr.msg.UserMessageMessage create(JsonParser p) { return new org.cnv.shr.msg.UserMessageMessage(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.Failure.getJsonName(), new JsonAllocator<org.cnv.shr.msg.Failure>()          
				{                                                                     
					public org.cnv.shr.msg.Failure create(JsonParser p) { return new org.cnv.shr.msg.Failure(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.Wait.getJsonName(), new JsonAllocator<org.cnv.shr.msg.Wait>()          
				{                                                                     
					public org.cnv.shr.msg.Wait create(JsonParser p) { return new org.cnv.shr.msg.Wait(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.HeartBeat.getJsonName(), new JsonAllocator<org.cnv.shr.msg.HeartBeat>()          
				{                                                                     
					public org.cnv.shr.msg.HeartBeat create(JsonParser p) { return new org.cnv.shr.msg.HeartBeat(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.LookingFor.getJsonName(), new JsonAllocator<org.cnv.shr.msg.LookingFor>()          
				{                                                                     
					public org.cnv.shr.msg.LookingFor create(JsonParser p) { return new org.cnv.shr.msg.LookingFor(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.ConnectionOpenAwk.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.ConnectionOpenAwk>()          
				{                                                                     
					public org.cnv.shr.msg.key.ConnectionOpenAwk create(JsonParser p) { return new org.cnv.shr.msg.key.ConnectionOpenAwk(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.NewKey.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.NewKey>()          
				{                                                                     
					public org.cnv.shr.msg.key.NewKey create(JsonParser p) { return new org.cnv.shr.msg.key.NewKey(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.RevokeKey.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.RevokeKey>()          
				{                                                                     
					public org.cnv.shr.msg.key.RevokeKey create(JsonParser p) { return new org.cnv.shr.msg.key.RevokeKey(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.ConnectionOpened.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.ConnectionOpened>()          
				{                                                                     
					public org.cnv.shr.msg.key.ConnectionOpened create(JsonParser p) { return new org.cnv.shr.msg.key.ConnectionOpened(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.KeyNotFound.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.KeyNotFound>()          
				{                                                                     
					public org.cnv.shr.msg.key.KeyNotFound create(JsonParser p) { return new org.cnv.shr.msg.key.KeyNotFound(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.KeyFailure.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.KeyFailure>()          
				{                                                                     
					public org.cnv.shr.msg.key.KeyFailure create(JsonParser p) { return new org.cnv.shr.msg.key.KeyFailure(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.OpenConnection.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.OpenConnection>()          
				{                                                                     
					public org.cnv.shr.msg.key.OpenConnection create(JsonParser p) { return new org.cnv.shr.msg.key.OpenConnection(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.KeyChange.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.KeyChange>()          
				{                                                                     
					public org.cnv.shr.msg.key.KeyChange create(JsonParser p) { return new org.cnv.shr.msg.key.KeyChange(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.WhoIAm.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.WhoIAm>()          
				{                                                                     
					public org.cnv.shr.msg.key.WhoIAm create(JsonParser p) { return new org.cnv.shr.msg.key.WhoIAm(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.EmptyMessage.getJsonName(), new JsonAllocator<org.cnv.shr.msg.EmptyMessage>()          
				{                                                                     
					public org.cnv.shr.msg.EmptyMessage create(JsonParser p) { return new org.cnv.shr.msg.EmptyMessage(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.DoneResponse.getJsonName(), new JsonAllocator<org.cnv.shr.msg.DoneResponse>()          
				{                                                                     
					public org.cnv.shr.msg.DoneResponse create(JsonParser p) { return new org.cnv.shr.msg.DoneResponse(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.ChecksumRequest.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.ChecksumRequest>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.ChecksumRequest create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChecksumRequest(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.ChecksumResponse.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.ChecksumResponse>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.ChecksumResponse create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChecksumResponse(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.dwn.NewAesKey.getJsonName(), new JsonAllocator<org.cnv.shr.msg.dwn.NewAesKey>()          
				{                                                                     
					public org.cnv.shr.msg.dwn.NewAesKey create(JsonParser p) { return new org.cnv.shr.msg.dwn.NewAesKey(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.key.PermissionFailure.getJsonName(), new JsonAllocator<org.cnv.shr.msg.key.PermissionFailure>()          
				{                                                                     
					public org.cnv.shr.msg.key.PermissionFailure create(JsonParser p) { return new org.cnv.shr.msg.key.PermissionFailure(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.GetPermission.getJsonName(), new JsonAllocator<org.cnv.shr.msg.GetPermission>()          
				{                                                                     
					public org.cnv.shr.msg.GetPermission create(JsonParser p) { return new org.cnv.shr.msg.GetPermission(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.GotPermission.getJsonName(), new JsonAllocator<org.cnv.shr.msg.GotPermission>()          
				{                                                                     
					public org.cnv.shr.msg.GotPermission create(JsonParser p) { return new org.cnv.shr.msg.GotPermission(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.swup.UpdateInfoMessage.getJsonName(), new JsonAllocator<org.cnv.shr.msg.swup.UpdateInfoMessage>()          
				{                                                                     
					public org.cnv.shr.msg.swup.UpdateInfoMessage create(JsonParser p) { return new org.cnv.shr.msg.swup.UpdateInfoMessage(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.swup.UpdateInfoRequest.getJsonName(), new JsonAllocator<org.cnv.shr.msg.swup.UpdateInfoRequest>()          
				{                                                                     
					public org.cnv.shr.msg.swup.UpdateInfoRequest create(JsonParser p) { return new org.cnv.shr.msg.swup.UpdateInfoRequest(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.swup.UpdateInfoRequestRequest.getJsonName(), new JsonAllocator<org.cnv.shr.msg.swup.UpdateInfoRequestRequest>()          
				{                                                                     
					public org.cnv.shr.msg.swup.UpdateInfoRequestRequest create(JsonParser p) { return new org.cnv.shr.msg.swup.UpdateInfoRequestRequest(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.msg.ShowApplication.getJsonName(), new JsonAllocator<org.cnv.shr.msg.ShowApplication>()          
				{                                                                     
					public org.cnv.shr.msg.ShowApplication create(JsonParser p) { return new org.cnv.shr.msg.ShowApplication(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.db.h2.bak.LocalBackup.getJsonName(), new JsonAllocator<org.cnv.shr.db.h2.bak.LocalBackup>()          
				{                                                                     
					public org.cnv.shr.db.h2.bak.LocalBackup create(JsonParser p) { return new org.cnv.shr.db.h2.bak.LocalBackup(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.db.h2.bak.MachineBackup.getJsonName(), new JsonAllocator<org.cnv.shr.db.h2.bak.MachineBackup>()          
				{                                                                     
					public org.cnv.shr.db.h2.bak.MachineBackup create(JsonParser p) { return new org.cnv.shr.db.h2.bak.MachineBackup(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.db.h2.bak.FileBackup.getJsonName(), new JsonAllocator<org.cnv.shr.db.h2.bak.FileBackup>()          
				{                                                                     
					public org.cnv.shr.db.h2.bak.FileBackup create(JsonParser p) { return new org.cnv.shr.db.h2.bak.FileBackup(p); }  
				});                                                                   
				ALLOCATORS.put(org.cnv.shr.db.h2.bak.RootPermissionBackup.getJsonName(), new JsonAllocator<org.cnv.shr.db.h2.bak.RootPermissionBackup>()          
				{                                                                     
					public org.cnv.shr.db.h2.bak.RootPermissionBackup create(JsonParser p) { return new org.cnv.shr.db.h2.bak.RootPermissionBackup(p); }  
				});                                                                   
                                     
	}
}
