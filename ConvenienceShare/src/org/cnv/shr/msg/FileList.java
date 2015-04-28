package org.cnv.shr.msg;

import java.io.File;
import java.util.LinkedList;

import org.cnv.shr.mdl.LocalDirectory;

public class FileList extends Message
{
	LinkedList<File> files;

	public FileList(LocalDirectory local)
	{

	}

	@Override
	public void perform()
	{
		// Machine m = Remotes.getInstance().getMachine(getMachine());
		// set remote directories

	}

}
