package org.cnv.shr.dmn.mn;

import org.cnv.shr.dmn.Services;

public abstract class Quiter
{
	private boolean quitting;
	
	
	public void quit()
	{
		if (quitting)
		{
			return;
		}
		else
		{
			quitting = true;
		}
		try
		{
			Services.deInitialize();
		}
		finally
		{
			doFinal();
		}
	}
	
	protected abstract void doFinal();
}