package org.cnv.shr.mdl;

public class RemoteDirectory extends RootDirectory
{
	public RemoteDirectory(Machine machine, String path, String tags, String description)
	{
		super(machine, path, tags, description);
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
}
