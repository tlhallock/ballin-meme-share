package org.cnv.shr.msg;

import java.io.IOException;

import org.cnv.shr.dmn.Locals;
import org.cnv.shr.mdl.LocalDirectory;

public class ListFiles extends Message
{

	@Override
	public void perform()
	{
		for (LocalDirectory local : Locals.getInstance().listLocals())
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
