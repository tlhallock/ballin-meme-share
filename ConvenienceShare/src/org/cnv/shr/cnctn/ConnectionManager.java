
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

import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.cnv.shr.cnctn.HandShake.HandShakeResults;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public class ConnectionManager
{
	private List<ConnectionRunnable> connectionRunnables = new LinkedList<>();
	
	// Would love to not hang up when someone tries to connect to a busy port...
//	private boolean[] freeSockets = new boolean[];
	
	
	static String getIp(String url)
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			return url;
		}
		return url.substring(0, index);
	}
	static int getPort(String url)
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			return Services.settings.servePortBeginE.get();
		}
		return Integer.parseInt(url.substring(index + 1, url.length())); 
	}
	public void openConnection(ConnectionParams params)
	{
		boolean submitted = false;
		try
		{
			// move these
			if (Services.blackList.contains(params.identifier))
			{
				LogWrapper.getLogger().info(params.identifier + " is a blacklisted machine.");
				return;
			}

			if (params.tryingToConnectToLocal())
			{
				LogWrapper.getLogger().info("Can't connect to local machine. No reason for this.");
				return;
			}

			Services.connectionThreads.execute(() -> {
				try
				{
					HandShakeResults results = HandshakeClient.initiateHandShake(params);
					if (results == null) return;
					try (Socket socket = new Socket(params.ip, results.port))
					{
						Communication connection = new Communication(
								socket,
								results.incoming,
								results.outgoing,
								results.ident,
								params.reason);

					Services.userThreads.execute(() -> {
						try
						{
							params.notifyOpened(connection);
							if (params.closeWhenDone())
							{
								connection.finish();
							}
						}
						catch (Exception e)
						{
							LogWrapper.getLogger().log(Level.INFO, "Unable to execute callback: " + params.reason, e);
							connection.finish();
						}
					});
					

					ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection);
					connectionRunnable.run();
					}
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().info("Unable to open connection: " + e.getMessage());
				}
				finally
				{
					params.ensureNotification();
				}
			});

			submitted = true;
		}
		finally
		{
			if (!submitted)
			{
				Services.userThreads.execute(() -> {
					params.notifyFailed();
				});
			}
		}
	}
	
	public void closeAll()
	{
		synchronized (connectionRunnables)
		{
			for (ConnectionRunnable r : connectionRunnables)
			{
				r.cleanup(true);
			}
		}
	}

	void add(ConnectionRunnable connectionRunnable)
	{
		synchronized (connectionRunnables)
		{
			if (connectionRunnables.add(connectionRunnable))
			{
				Services.notifications.connectionOpened(connectionRunnable.connection);
			}
		}
	}
	void remove(ConnectionRunnable connectionRunnable)
	{
		synchronized (connectionRunnables)
		{
			if (connectionRunnables.remove(connectionRunnable))
			{
				Services.notifications.connectionClosed(connectionRunnable.connection);
			}
		}
	}
	
	
	

//
//public Communication openConnection(String url, boolean acceptKeys, String reason) throws UnknownHostException, IOException
//{
//	return openConnection(null, null, getIp(url), getPort(url), 1, null, acceptKeys, reason);
//}
//
//public Communication openConnection(JFrame origin, Machine m, boolean acceptKeys, String reason) throws UnknownHostException, IOException
//{
//	return openConnection(origin, m.getIdentifier(), m.getIp(), m.getPort(), m.getNumberOfPorts(), DbKeys.getKey(m), acceptKeys, reason);
//}
//
//public Communication openConnection(JFrame origin, Machine m, String reason) throws UnknownHostException, IOException
//{
//	return openConnection(origin, m.getIdentifier(), m.getIp(), m.getPort(), m.getNumberOfPorts(), DbKeys.getKey(m), acceptKeys, reason);
//}
//
//try (Communication connection = connect(params, authentication);)
//{
//	if (connection == null)
//	{
//		return;
//	}
//	connection.setRemoteIdentifier(params.identifier);
//	connection.setReason(params.reason);
//	connection.send(new WhoIAm());
//	connection.send(new ConnectionReason(params.reason));
//	connection.send(new OpenConnection(params.remoteKey, IdkWhereToPutThis.createTestNaunce(authentication, params.remoteKey)));
//	ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection, authentication);
//	connectionRunnable.run();
//}
//catch (Exception e)
//{
//	LogWrapper.getLogger().info("Unable to open connection: " + e.getMessage());
//}
//finally
//{
//	params.ensureNotification();
//}
}
