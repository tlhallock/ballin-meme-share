package org.cnv.shr.mdl;

import java.io.File;


public class RemoteFile extends SharedFile
{
	private String localCopy;

	public RemoteFile(int int1)
	{
		super(int1);
	}

	public RemoteFile(RemoteDirectory root, PathElement pathElement,
			long fileSize, String checksum, String tags, long lastModified)
	{
		super(null);
		rootDirectory = root;
		path = pathElement;
		this.fileSize = fileSize;
		this.checksum = checksum;
		this.tags = tags;
		this.lastModified = lastModified;
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
	
	public File getTargetFile()
	{
		return new File(((RemoteDirectory) getRootDirectory()).getLocalRoot().getAbsolutePath()
				+ File.separator + getPath().getFullPath());
	}
}
