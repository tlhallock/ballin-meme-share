
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.cnctn;

import java.io.IOException;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import org.cnv.shr.cnctn.ConnectionParams.AutoCloseConnectionParams;
import org.cnv.shr.dmn.Services;
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
	
	// TODO: use this
	private static TimerTask getAttempter()
	{
		return new TimerTask() {
			@Override
			public void run()
			{
				Services.userThreads.execute(() -> {
					attemptAuthentications(Services.keyManager.getPendingAuthenticationRequests());
				});
			}};
	}
	
	public static void attemptAuthentications(HashSet<String> pendingAuthenticationRequests)
	{
		for (String url : (Set<String>) pendingAuthenticationRequests.clone())
		{
			Services.networkManager.openConnection(new AutoCloseConnectionParams(url, true, "Re-attempt to add machine")
			{
				@Override
				public void opened(Communication connection) throws Exception
				{
					synchronized (pendingAuthenticationRequests)
					{
						pendingAuthenticationRequests.remove(url);
					}
					Services.keyManager.writeKeys(Services.settings.keysFile.getPath());
				}
			});
		}
	}
}
