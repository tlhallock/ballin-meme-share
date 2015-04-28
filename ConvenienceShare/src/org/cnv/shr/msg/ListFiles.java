package org.cnv.shr.msg;

import java.io.IOException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;

public class ListFiles extends Message
{

	@Override
	public void perform()
	{
		for (LocalDirectory local : Services.locals.listLocals())
		{
			try
			{
				new FileList(local).send(getMachine());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
