package org.cnv.shr.mdl;

public class RemoteDirectory extends RootDirectory
{
	public RemoteDirectory(Machine machine, String path)
	{
		super(machine, path);
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
