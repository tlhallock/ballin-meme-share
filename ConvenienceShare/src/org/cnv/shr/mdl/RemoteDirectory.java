package org.cnv.shr.mdl;

import java.io.File;
import java.io.IOException;
import java.util.Timer;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.lcl.FileSource;
import org.cnv.shr.lcl.RootSynchronizer;
import org.cnv.shr.lcl.FileSource.FileFileSource;
import org.cnv.shr.lcl.RemoteSynchronizer;


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
		Timer t = new Timer();
		RootSynchronizer localSynchronizer;
		try
		{
			localSynchronizer = new RemoteSynchronizer(this);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		t.scheduleAtFixedRate(localSynchronizer, RootSynchronizer.DEBUG_REPEAT, RootSynchronizer.DEBUG_REPEAT);
		localSynchronizer.synchronize();
		t.cancel();
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
