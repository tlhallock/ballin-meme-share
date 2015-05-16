package org.cnv.shr.dmn.dwn;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;

public class SharedFileId
{
	private String machineIdent;
	private String rootName;
	private String path;
	private String checksum;
	
	public SharedFileId(SharedFile file)
	{
		this.machineIdent = file.getRootDirectory().getMachine().getIdentifier();
		this.rootName = file.getRootDirectory().getName();
		this.path = file.getPath().getFullPath();
		this.checksum = file.getChecksum();
	}

	public SharedFileId(String readString, String readString2, String readString3, String readString4)
	{
		this.machineIdent = readString;
		this.rootName = readString2;
		this.path = readString3;
		this.checksum = readString4;
		if (this.checksum.length() == 0)
		{
			this.checksum = null;
		}
	}

	public String getMachineIdent()
	{
		return machineIdent;
	}

	public String getRootName()
	{
		return rootName;
	}

	public String getPath()
	{
		return path;
	}

	public String getChecksum()
	{
		if (checksum == null)
		{
			return "";
		}
		return checksum;
	}
	
	public int hashCode()
	{
		return (machineIdent + ":" + rootName + ":" + path).hashCode();
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof SharedFileId))
		{
			return false;
		}
		SharedFileId o = (SharedFileId) other;
		return machineIdent.equals(o.machineIdent)
				&& rootName.equals(o.rootName)
				&& path.equals(path)
				&& (checksum == null || o.checksum == null || checksum.equals(o.checksum));
	}
	
	public LocalFile getLocal()
	{
		PathElement pathElement = DbPaths.getPathElement(path);
		LocalDirectory local = DbRoots.getLocalByName(rootName);
		return (LocalFile) DbFiles.getFile(local, pathElement);
	}
	
	public RemoteFile getRemote()
	{
		Machine machine = DbMachines.getMachine(machineIdent);
		RootDirectory root = DbRoots.getRoot(machine, path);
		PathElement pathElement = DbPaths.getPathElement(path);
		return (RemoteFile) DbFiles.getFile(root, pathElement);
	}
	
	public String toString()
	{
		return machineIdent + " : " + rootName + " : " + path + " : " + checksum;
	}
}
