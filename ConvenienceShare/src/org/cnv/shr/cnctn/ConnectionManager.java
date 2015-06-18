
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
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.OpenConnection;
import org.cnv.shr.msg.key.WhoIAm;
import org.cnv.shr.util.LogWrapper;

public class ConnectionManager
{
	private List<ConnectionRunnable> connectionRunnables = new LinkedList<>();
	
	public Communication openConnection(String url, boolean acceptKeys) throws UnknownHostException, IOException
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			// Should try all other ports too...
			return openConnection(url, Services.settings.servePortBeginE.get(), 1, null, acceptKeys);
		}
		return openConnection(url.substring(0, index), Integer.parseInt(url.substring(index + 1, url.length())), 1, null, acceptKeys);
	}
	public Communication openConnection(Machine m, boolean acceptKeys) throws UnknownHostException, IOException
	{
		return openConnection(m.getIp(), m.getPort(), m.getNumberOfPorts(), DbKeys.getKey(m), acceptKeys);
	}
	
	private static Communication openConnection(String ip, 
			int portBegin,
			int numPorts,
			final PublicKey remoteKey, 
			boolean acceptAnyKeys) throws UnknownHostException, IOException
	{
		Authenticator authentication = new Authenticator(acceptAnyKeys, remoteKey, Services.keyManager.getPublicKey());
		class TmpObject
		{
			Communication connection;
		}
		TmpObject o = new TmpObject();
		
		Services.connectionThreads.execute(new Runnable()
		{
			public void run()
			{
				try (Communication connection = connect(authentication, ip, portBegin, portBegin + Math.min(50, numPorts));)
				{
					o.connection = connection;
					if (connection == null)
					{
						return;
					}
					connection.send(new WhoIAm());
					connection.send(new OpenConnection(remoteKey, IdkWhereToPutThis.createTestNaunce(authentication, remoteKey)));
					ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection, authentication);
					connectionRunnable.run();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().info("Unable to open connection: " + e.getMessage());
				}
			}
		});
		try
		{
			return authentication.waitForAuthentication() ? o.connection : null;
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().info("Unable to wait for connection: " + ex.getMessage());
			return null;
		}
	}
	
	private static Communication connect(Authenticator authentication, String ip, int portBegin, int portEnd) throws UnknownHostException, IOException
	{
		ArrayList<Integer> possibles = new ArrayList<>(portEnd - portBegin);
		for (int port = portBegin; port < portEnd; port++)
		{
			possibles.add(port);
		}
		Collections.shuffle(possibles);
		for (int port : possibles)
		{
			try
			{
				return new Communication(authentication, ip, port);
			}
			catch (ConnectException ex)
			{
				LogWrapper.getLogger().info("Unable to connect to " + ip + " on port " + port + ", trying others if available. " + ex.getMessage());
			}
		}
		return null;
	}
	
	public void handleConnection(Socket accepted) throws IOException
	{
		Authenticator authentication = new Authenticator();
		try (Communication connection = new Communication(authentication, accepted);)
		{
			connection.send(new WhoIAm());
			ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection, authentication);
			connectionRunnable.run();
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
}
