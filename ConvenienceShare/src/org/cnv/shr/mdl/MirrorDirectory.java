package org.cnv.shr.mdl;

import java.io.IOException;
import java.nio.file.Path;

import org.cnv.shr.db.h2.SharingState;

public class MirrorDirectory extends LocalDirectory
{
	public MirrorDirectory(String name,
			String description, 
			String tags, 
			long minFSize,
			long maxFSize, 
			String path2, 
			SharingState defaultSharingState,
			Long totalFileSize,
			Long totalNumFiles)
	{
		super(name, description, tags, minFSize, maxFSize, path2, defaultSharingState, totalFileSize, totalNumFiles);
	}
	
	public MirrorDirectory(Path path, String name) throws IOException
	{
		super(path, name);
	}
	
	public MirrorDirectory(Integer id)
	{
		super(id);
	}

	public RootDirectoryType getType()
	{
		return RootDirectoryType.MIRROR;
	}
}
