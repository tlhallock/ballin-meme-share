package org.cnv.shr.mdl;

import java.io.File;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;


public class RemoteDirectory extends RootDirectory
{
	PathElement path;
	
	public RemoteDirectory(Machine machine, String name, String tags, String description)
	{
		super(machine, name, tags, description);
		path = DbPaths.getPathElement(Services.settings.downloadsDirectory.get().getAbsolutePath() + "/" + getName());
	}

	public RemoteDirectory(int int1)
	{
		super(int1);
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
	

	@Override
	public void synchronizeInternal()
	{
		
	}
	
	@Override
	public PathElement getCanonicalPath()
	{
		return path;
	}

	@Override
	protected void setPath(PathElement object)
	{
		this.path = object;
	}

	public File getLocalRoot()
	{
		return new File(path.getFullPath());
	}
}
