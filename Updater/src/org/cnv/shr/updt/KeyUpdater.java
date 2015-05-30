package org.cnv.shr.updt;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;

public class KeyUpdater extends TimerTask
{
	@Override
	public void run()
	{
		try
		{
			Updater.service.createAnotherKey(Updater.keysFile, Updater.KEY_LENGTH);
		}
		catch (NoSuchAlgorithmException | NoSuchProviderException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "No provider", e);
			System.exit(-1);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to create another key.", e);
		}
	}
}
