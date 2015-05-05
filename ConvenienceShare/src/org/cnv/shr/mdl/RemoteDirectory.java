package org.cnv.shr.mdl;

import org.cnv.shr.db.h2.DbPaths;


public class RemoteDirectory extends RootDirectory
{
	PathElement path;
	
	public RemoteDirectory(Machine machine, String name, String tags, String description)
	{
		super(machine, name, tags, description);
		path = DbPaths.getPathElement("remote:" + machine.getIdentifier() + "//" + name);
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
}
