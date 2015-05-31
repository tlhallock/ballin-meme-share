package org.cnv.shr.track;

import java.io.File;
import java.io.OutputStream;

import org.cnv.shr.trck.Comment;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;

public class TrackerStore
{
	private File trackerFile;
	private File machinesFile;
	private File filesFile;
	private File commentsFile;

	// Queries
	public static void listMachines(OutputStream output)
	{
		
	}
	
	public static void listTrackers(OutputStream output)
	{
		
	}
	
	public static void listMachines(FileEntry entry, OutputStream output)
	{
		
	}
	
	public static void listComments(MachineEntry entry)
	{
		
	}
	
	public void postComment(Comment commenet)
	{
		
	}
	
	
	public static MachineEntry getMachine(String ident)
	{
		return null;
	}
	

	// status
	public static void machineFound(MachineEntry machine, long now)
	{
		
	}
	
	
	// entries
	public static void machineClaims(MachineEntry machine, FileEntry file)
	{
		
	}
	
	public static void machineLost(MachineEntry machine, FileEntry file)
	{
		
	}
}
