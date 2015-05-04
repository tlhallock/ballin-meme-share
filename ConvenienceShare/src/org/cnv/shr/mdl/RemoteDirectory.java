package org.cnv.shr.mdl;

public class RemoteDirectory extends RootDirectory
{
	public RemoteDirectory(Machine machine, String path, String tags, String description)
	{
		super(machine, path, tags, description);
	}

	public RemoteDirectory(int int1)
	{
		// TODO Auto-generated constructor stub
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
