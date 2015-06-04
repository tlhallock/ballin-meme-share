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
	// Either
	private String machineIdent;
	private String rootName;
	private String path;
	
	public SharedFileId(SharedFile file)
	{
		this.machineIdent = file.getRootDirectory().getMachine().getIdentifier();
		this.rootName = file.getRootDirectory().getName();
		this.path = file.getPath().getFullPath();
	}

	public SharedFileId(String readString, String readString2, String readString3)
	{
		this.machineIdent = readString;
		this.rootName = readString2;
		this.path = readString3;
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

	@Override
	public int hashCode()
	{
		return (machineIdent + ":" + rootName + ":" + path).hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof SharedFileId))
		{
			return false;
		}
		SharedFileId o = (SharedFileId) other;
		return machineIdent.equals(o.machineIdent)
				&& rootName.equals(o.rootName)
				&& path.equals(path);
	}
	
	public LocalFile getLocal()
	{
		PathElement pathElement = DbPaths.getPathElement(path);
		LocalDirectory local = DbRoots.getLocalByName(rootName);
		return DbFiles.getFile(local, pathElement);
	}
	
	public RemoteFile getRemote()
	{
		Machine machine = DbMachines.getMachine(machineIdent);
		RootDirectory root = DbRoots.getRoot(machine, path);
		PathElement pathElement = DbPaths.getPathElement(path);
		return (RemoteFile) DbFiles.getFile(root, pathElement);
	}
	
	@Override
	public String toString()
	{
		return machineIdent + " : " + rootName + " : " + path;
	}
}
