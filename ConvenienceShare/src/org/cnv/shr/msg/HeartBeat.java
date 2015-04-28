package org.cnv.shr.msg;

import org.cnv.shr.dmn.Services;

public class HeartBeat extends Message
{

	@Override
	public void perform()
	{
		Services.remotes.isAlive(getMachine());
	}

}
