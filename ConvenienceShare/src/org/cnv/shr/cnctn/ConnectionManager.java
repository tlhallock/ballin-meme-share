
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.AlternativeAddresses;
import org.cnv.shr.msg.key.ConnectionReason;
import org.cnv.shr.msg.key.OpenConnection;
import org.cnv.shr.msg.key.WhoIAm;
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
//	
//	public Communication openConnection(String url, boolean acceptKeys, String reason) throws UnknownHostException, IOException
//	{
//		return openConnection(null, null, getIp(url), getPort(url), 1, null, acceptKeys, reason);
//	}
//
//	public Communication openConnection(JFrame origin, Machine m, boolean acceptKeys, String reason) throws UnknownHostException, IOException
//	{
//		return openConnection(origin, m.getIdentifier(), m.getIp(), m.getPort(), m.getNumberOfPorts(), DbKeys.getKey(m), acceptKeys, reason);
//	}
//
//	public Communication openConnection(JFrame origin, Machine m, String reason) throws UnknownHostException, IOException
//	{
//		return openConnection(origin, m.getIdentifier(), m.getIp(), m.getPort(), m.getNumberOfPorts(), DbKeys.getKey(m), acceptKeys, reason);
//	}
//	
	public void openConnection(ConnectionParams params)
	{
		boolean submitted = false;
		try
		{
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

			Authenticator authentication = new Authenticator(params);

			Services.connectionThreads.execute(() -> {
				try (Communication connection = connect(params, authentication);)
				{
					if (connection == null)
					{
						return;
					}
					connection.setRemoteIdentifier(params.identifier);
					connection.setReason(params.reason);
					connection.send(new WhoIAm());
					connection.send(new ConnectionReason(params.reason));
					connection.send(new OpenConnection(params.remoteKey, IdkWhereToPutThis.createTestNaunce(authentication, params.remoteKey)));
					ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection, authentication);
					connectionRunnable.run();
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
	
	private static Communication connect(ConnectionParams params, Authenticator authentication) throws UnknownHostException, IOException
	{
		ArrayList<Integer> possibles = new ArrayList<>(params.getPortEnd() - params.portBegin);
		for (int port = params.portBegin; port < params.getPortEnd(); port++)
		{
			possibles.add(port);
		}
		Collections.shuffle(possibles);
		for (int port : possibles)
		{
			try
			{
				return new Communication(authentication, params.ip, port);
			}
			catch (ConnectException ex)
			{
				LogWrapper.getLogger().info("Unable to connect to " + params.ip + " on port " + port + ", trying others if available. " + ex.getMessage());
			}
		}
		if (params.origin == null || params.identifier == null)
		{
			return null;
		}
		if (Services.trackers.getClients().isEmpty())
		{
			JOptionPane.showMessageDialog(params.origin, "ConvenienceShare was unable to connect to "
					+ params.identifier + " at " + params.ip + "[" + params.portBegin + "-" + params.getPortEnd() + "].\n",
					"Unable to connect",
					JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(params.origin, "ConvenienceShare was unable to connect to "
				+ params.identifier + " at " + params.ip + "[" + params.portBegin + "-" + params.getPortEnd() + "].\n"
				+ "Would you like to connect to a tracker to see if the ip address has changed?",
				"Unable to connect",
				JOptionPane.YES_NO_OPTION))
		{
			return null;
		}
		
		AlternativeAddresses findAlternativeUrls = Services.trackers.findAlternativeUrls(params.identifier);
		findAlternativeUrls.remove(params.ip, params.portBegin, params.getPortEnd());
		if (findAlternativeUrls.isEmpty())
		{
			JOptionPane.showConfirmDialog(
					params.origin, 
					"No other addresses found.",
					"Unable to connect.",
					JOptionPane.WARNING_MESSAGE);
				return null;
		}
		
		for (String alternative : findAlternativeUrls.getIps())
		{
			if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
					params.origin, 
					"A tracker listed another url listed for " + params.identifier + " at " + findAlternativeUrls.describe(alternative) + ".\n"
						+ "Would you like to try this one?",
					"Found another address",
					JOptionPane.YES_NO_OPTION))
			{
				continue;
			}
			
			for (Integer port : findAlternativeUrls.getPorts(alternative))
			{
				try
				{
					return new Communication(authentication, alternative, port);
				}
				catch (ConnectException ex)
				{
					LogWrapper.getLogger().info("Unable to connect to " + alternative + " on port " + port + ", trying others if available. " + ex.getMessage());
				}
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
