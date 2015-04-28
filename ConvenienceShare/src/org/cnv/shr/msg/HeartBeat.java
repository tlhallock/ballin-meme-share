package org.cnv.shr.msg;

import org.cnv.shr.dmn.Remotes;

public class HeartBeat extends Message
{

	@Override
	public void perform()
	{
		Remotes.getInstance().isAlive(getMachine());
	}

}
