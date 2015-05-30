package org.cnv.shr.cnctn;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class IdkWhereToPutThis
{
	public static byte[] createTestNaunce(Authenticator authentication, PublicKey remoteKey) throws IOException
	{
		if (remoteKey == null)
		{
			LogWrapper.getLogger().info("Unable to create naunce: remote key is null.");
			return new byte[0];
		}
		final byte[] original = Misc.createNaunce(Services.settings.minNaunce.get());
		final byte[] sentNaunce = Services.keyManager.encrypt(remoteKey, original);
		authentication.addPendingNaunce(original);
		return sentNaunce;
	}
	
	
	public static TimerTask getAttempter()
	{
		return new TimerTask()
		{
			@Override
			public void run()
			{
				try
				{
					attemptAuthentications(Services.keyManager.getPendingAuthenticationRequests());
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
	}

	
	public static void attemptAuthentications(Set<String> pendingAuthenticationRequests) throws IOException
	{
		outer: for (;;)
		{
			for (String url : pendingAuthenticationRequests)
			{
				Communication openConnection;
				try
				{
					openConnection = Services.networkManager.openConnection(url, true);
					if (openConnection != null)
					{
						openConnection.send(new FindMachines());
						openConnection.send(new MachineFound());
						pendingAuthenticationRequests.remove(url);
						Services.keyManager.writeKeys(Services.settings.keysFile.get());
						continue outer;
					}
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Failed authentications again", e);
					continue;
				}
			}
		}
	}
}
