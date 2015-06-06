package org.cnv.shr.json;

import java.util.HashMap;

import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.DoneResponse;
import org.cnv.shr.msg.EmptyMessage;
import org.cnv.shr.msg.Failure;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.GetPermission;
import org.cnv.shr.msg.GotPermission;
import org.cnv.shr.msg.HeartBeat;
import org.cnv.shr.msg.ListPath;
import org.cnv.shr.msg.ListRoots;
import org.cnv.shr.msg.LookingFor;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.msg.PathList;
import org.cnv.shr.msg.PathListChild;
import org.cnv.shr.msg.RootList;
import org.cnv.shr.msg.RootListChild;
import org.cnv.shr.msg.ShowApplication;
import org.cnv.shr.msg.UserMessageMessage;
import org.cnv.shr.msg.Wait;
import org.cnv.shr.util.Jsonable;

public class JsonAllocators
{
	private interface JsonAllocator<T extends Jsonable>
	{
		T create(JsonParser p);
	}
	
	
	public static <T extends Jsonable> T create(Class<T> clazz, JsonParser p)
	{
		return (T) ALLOCATORS.get(clazz.getName()).create(p);
	}
	public static Jsonable create(String className, JsonParser p)
	{
		return ALLOCATORS.get(className).create(p);
	}
	
	
	private static final HashMap<String, JsonAllocator<?>> ALLOCATORS = new HashMap<>();
	
	
	static
	{                                                                      
		ALLOCATORS.put("org.cnv.shr.db.h2.SharingState", new JsonAllocator<org.cnv.shr.db.h2.SharingState>()          
		{                                                                     
			public org.cnv.shr.db.h2.SharingState create(JsonParser p) { return SharingState.valueOf(p.getString()); }  
		});
		
		
		
		
		
		
					ALLOCATORS.put("org.cnv.shr.trck.TrackerEntry", new JsonAllocator<org.cnv.shr.trck.TrackerEntry>()          
					{                                                                     
						public org.cnv.shr.trck.TrackerEntry create(JsonParser p) { return new org.cnv.shr.trck.TrackerEntry(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.trck.MachineEntry", new JsonAllocator<org.cnv.shr.trck.MachineEntry>()          
					{                                                                     
						public org.cnv.shr.trck.MachineEntry create(JsonParser p) { return new org.cnv.shr.trck.MachineEntry(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.trck.CommentEntry", new JsonAllocator<org.cnv.shr.trck.CommentEntry>()          
					{                                                                     
						public org.cnv.shr.trck.CommentEntry create(JsonParser p) { return new org.cnv.shr.trck.CommentEntry(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.trck.FileEntry", new JsonAllocator<org.cnv.shr.trck.FileEntry>()          
					{                                                                     
						public org.cnv.shr.trck.FileEntry create(JsonParser p) { return new org.cnv.shr.trck.FileEntry(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.trck.Done", new JsonAllocator<org.cnv.shr.trck.Done>()          
					{                                                                     
						public org.cnv.shr.trck.Done create(JsonParser p) { return new org.cnv.shr.trck.Done(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.dmn.dwn.Chunk", new JsonAllocator<org.cnv.shr.dmn.dwn.Chunk>()          
					{                                                                     
						public org.cnv.shr.dmn.dwn.Chunk create(JsonParser p) { return new org.cnv.shr.dmn.dwn.Chunk(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.dmn.dwn.SharedFileId", new JsonAllocator<org.cnv.shr.dmn.dwn.SharedFileId>()          
					{                                                                     
						public org.cnv.shr.dmn.dwn.SharedFileId create(JsonParser p) { return new org.cnv.shr.dmn.dwn.SharedFileId(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.PathListChild", new JsonAllocator<org.cnv.shr.msg.PathListChild>()          
					{                                                                     
						public org.cnv.shr.msg.PathListChild create(JsonParser p) { return new org.cnv.shr.msg.PathListChild(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.RootListChild", new JsonAllocator<org.cnv.shr.msg.RootListChild>()          
					{                                                                     
						public org.cnv.shr.msg.RootListChild create(JsonParser p) { return new org.cnv.shr.msg.RootListChild(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.DownloadFailure", new JsonAllocator<org.cnv.shr.msg.dwn.DownloadFailure>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.DownloadFailure create(JsonParser p) { return new org.cnv.shr.msg.dwn.DownloadFailure(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.ChunkList", new JsonAllocator<org.cnv.shr.msg.dwn.ChunkList>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.ChunkList create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChunkList(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.DownloadDone", new JsonAllocator<org.cnv.shr.msg.dwn.DownloadDone>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.DownloadDone create(JsonParser p) { return new org.cnv.shr.msg.dwn.DownloadDone(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.CompletionStatus", new JsonAllocator<org.cnv.shr.msg.dwn.CompletionStatus>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.CompletionStatus create(JsonParser p) { return new org.cnv.shr.msg.dwn.CompletionStatus(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.FileRequest", new JsonAllocator<org.cnv.shr.msg.dwn.FileRequest>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.FileRequest create(JsonParser p) { return new org.cnv.shr.msg.dwn.FileRequest(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.ChunkResponse", new JsonAllocator<org.cnv.shr.msg.dwn.ChunkResponse>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.ChunkResponse create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChunkResponse(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.RequestCompletionStatus", new JsonAllocator<org.cnv.shr.msg.dwn.RequestCompletionStatus>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.RequestCompletionStatus create(JsonParser p) { return new org.cnv.shr.msg.dwn.RequestCompletionStatus(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.ChunkRequest", new JsonAllocator<org.cnv.shr.msg.dwn.ChunkRequest>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.ChunkRequest create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChunkRequest(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.MachineHasFile", new JsonAllocator<org.cnv.shr.msg.dwn.MachineHasFile>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.MachineHasFile create(JsonParser p) { return new org.cnv.shr.msg.dwn.MachineHasFile(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.ListRoots", new JsonAllocator<org.cnv.shr.msg.ListRoots>()          
					{                                                                     
						public org.cnv.shr.msg.ListRoots create(JsonParser p) { return new org.cnv.shr.msg.ListRoots(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.ListPath", new JsonAllocator<org.cnv.shr.msg.ListPath>()          
					{                                                                     
						public org.cnv.shr.msg.ListPath create(JsonParser p) { return new org.cnv.shr.msg.ListPath(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.MachineFound", new JsonAllocator<org.cnv.shr.msg.MachineFound>()          
					{                                                                     
						public org.cnv.shr.msg.MachineFound create(JsonParser p) { return new org.cnv.shr.msg.MachineFound(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.DoneMessage", new JsonAllocator<org.cnv.shr.msg.DoneMessage>()          
					{                                                                     
						public org.cnv.shr.msg.DoneMessage create(JsonParser p) { return new org.cnv.shr.msg.DoneMessage(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.PathList", new JsonAllocator<org.cnv.shr.msg.PathList>()          
					{                                                                     
						public org.cnv.shr.msg.PathList create(JsonParser p) { return new org.cnv.shr.msg.PathList(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.RootList", new JsonAllocator<org.cnv.shr.msg.RootList>()          
					{                                                                     
						public org.cnv.shr.msg.RootList create(JsonParser p) { return new org.cnv.shr.msg.RootList(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.FindMachines", new JsonAllocator<org.cnv.shr.msg.FindMachines>()          
					{                                                                     
						public org.cnv.shr.msg.FindMachines create(JsonParser p) { return new org.cnv.shr.msg.FindMachines(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.UserMessageMessage", new JsonAllocator<org.cnv.shr.msg.UserMessageMessage>()          
					{                                                                     
						public org.cnv.shr.msg.UserMessageMessage create(JsonParser p) { return new org.cnv.shr.msg.UserMessageMessage(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.Failure", new JsonAllocator<org.cnv.shr.msg.Failure>()          
					{                                                                     
						public org.cnv.shr.msg.Failure create(JsonParser p) { return new org.cnv.shr.msg.Failure(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.Wait", new JsonAllocator<org.cnv.shr.msg.Wait>()          
					{                                                                     
						public org.cnv.shr.msg.Wait create(JsonParser p) { return new org.cnv.shr.msg.Wait(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.HeartBeat", new JsonAllocator<org.cnv.shr.msg.HeartBeat>()          
					{                                                                     
						public org.cnv.shr.msg.HeartBeat create(JsonParser p) { return new org.cnv.shr.msg.HeartBeat(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.LookingFor", new JsonAllocator<org.cnv.shr.msg.LookingFor>()          
					{                                                                     
						public org.cnv.shr.msg.LookingFor create(JsonParser p) { return new org.cnv.shr.msg.LookingFor(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.ConnectionOpenAwk", new JsonAllocator<org.cnv.shr.msg.key.ConnectionOpenAwk>()          
					{                                                                     
						public org.cnv.shr.msg.key.ConnectionOpenAwk create(JsonParser p) { return new org.cnv.shr.msg.key.ConnectionOpenAwk(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.NewKey", new JsonAllocator<org.cnv.shr.msg.key.NewKey>()          
					{                                                                     
						public org.cnv.shr.msg.key.NewKey create(JsonParser p) { return new org.cnv.shr.msg.key.NewKey(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.RevokeKey", new JsonAllocator<org.cnv.shr.msg.key.RevokeKey>()          
					{                                                                     
						public org.cnv.shr.msg.key.RevokeKey create(JsonParser p) { return new org.cnv.shr.msg.key.RevokeKey(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.ConnectionOpened", new JsonAllocator<org.cnv.shr.msg.key.ConnectionOpened>()          
					{                                                                     
						public org.cnv.shr.msg.key.ConnectionOpened create(JsonParser p) { return new org.cnv.shr.msg.key.ConnectionOpened(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.KeyNotFound", new JsonAllocator<org.cnv.shr.msg.key.KeyNotFound>()          
					{                                                                     
						public org.cnv.shr.msg.key.KeyNotFound create(JsonParser p) { return new org.cnv.shr.msg.key.KeyNotFound(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.KeyFailure", new JsonAllocator<org.cnv.shr.msg.key.KeyFailure>()          
					{                                                                     
						public org.cnv.shr.msg.key.KeyFailure create(JsonParser p) { return new org.cnv.shr.msg.key.KeyFailure(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.OpenConnection", new JsonAllocator<org.cnv.shr.msg.key.OpenConnection>()          
					{                                                                     
						public org.cnv.shr.msg.key.OpenConnection create(JsonParser p) { return new org.cnv.shr.msg.key.OpenConnection(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.KeyChange", new JsonAllocator<org.cnv.shr.msg.key.KeyChange>()          
					{                                                                     
						public org.cnv.shr.msg.key.KeyChange create(JsonParser p) { return new org.cnv.shr.msg.key.KeyChange(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.WhoIAm", new JsonAllocator<org.cnv.shr.msg.key.WhoIAm>()          
					{                                                                     
						public org.cnv.shr.msg.key.WhoIAm create(JsonParser p) { return new org.cnv.shr.msg.key.WhoIAm(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.EmptyMessage", new JsonAllocator<org.cnv.shr.msg.EmptyMessage>()          
					{                                                                     
						public org.cnv.shr.msg.EmptyMessage create(JsonParser p) { return new org.cnv.shr.msg.EmptyMessage(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.DoneResponse", new JsonAllocator<org.cnv.shr.msg.DoneResponse>()          
					{                                                                     
						public org.cnv.shr.msg.DoneResponse create(JsonParser p) { return new org.cnv.shr.msg.DoneResponse(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.ChecksumRequest", new JsonAllocator<org.cnv.shr.msg.dwn.ChecksumRequest>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.ChecksumRequest create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChecksumRequest(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.ChecksumResponse", new JsonAllocator<org.cnv.shr.msg.dwn.ChecksumResponse>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.ChecksumResponse create(JsonParser p) { return new org.cnv.shr.msg.dwn.ChecksumResponse(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.dwn.NewAesKey", new JsonAllocator<org.cnv.shr.msg.dwn.NewAesKey>()          
					{                                                                     
						public org.cnv.shr.msg.dwn.NewAesKey create(JsonParser p) { return new org.cnv.shr.msg.dwn.NewAesKey(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.key.PermissionFailure", new JsonAllocator<org.cnv.shr.msg.key.PermissionFailure>()          
					{                                                                     
						public org.cnv.shr.msg.key.PermissionFailure create(JsonParser p) { return new org.cnv.shr.msg.key.PermissionFailure(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.GetPermission", new JsonAllocator<org.cnv.shr.msg.GetPermission>()          
					{                                                                     
						public org.cnv.shr.msg.GetPermission create(JsonParser p) { return new org.cnv.shr.msg.GetPermission(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.GotPermission", new JsonAllocator<org.cnv.shr.msg.GotPermission>()          
					{                                                                     
						public org.cnv.shr.msg.GotPermission create(JsonParser p) { return new org.cnv.shr.msg.GotPermission(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.swup.UpdateInfoMessage", new JsonAllocator<org.cnv.shr.msg.swup.UpdateInfoMessage>()          
					{                                                                     
						public org.cnv.shr.msg.swup.UpdateInfoMessage create(JsonParser p) { return new org.cnv.shr.msg.swup.UpdateInfoMessage(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.swup.UpdateInfoRequest", new JsonAllocator<org.cnv.shr.msg.swup.UpdateInfoRequest>()          
					{                                                                     
						public org.cnv.shr.msg.swup.UpdateInfoRequest create(JsonParser p) { return new org.cnv.shr.msg.swup.UpdateInfoRequest(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.swup.UpdateInfoRequestRequest", new JsonAllocator<org.cnv.shr.msg.swup.UpdateInfoRequestRequest>()          
					{                                                                     
						public org.cnv.shr.msg.swup.UpdateInfoRequestRequest create(JsonParser p) { return new org.cnv.shr.msg.swup.UpdateInfoRequestRequest(p); }  
					});                                                                   
					ALLOCATORS.put("org.cnv.shr.msg.ShowApplication", new JsonAllocator<org.cnv.shr.msg.ShowApplication>()          
					{                                                                     
						public org.cnv.shr.msg.ShowApplication create(JsonParser p) { return new org.cnv.shr.msg.ShowApplication(p); }  
					});                                                                 
	}
}
