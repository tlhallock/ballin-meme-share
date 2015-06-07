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
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.OpenConnection;
import org.cnv.shr.msg.key.WhoIAm;
import org.cnv.shr.util.LogWrapper;

public class ConnectionManager
{
	List<ConnectionRunnable> runnables = new LinkedList<>();
	
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
	
	private synchronized Communication openConnection(String ip, 
			int portBegin,
			int numPorts,
			final PublicKey remoteKey, 
			boolean acceptAnyKeys) throws UnknownHostException, IOException
	{
		Authenticator authentication = new Authenticator(acceptAnyKeys, remoteKey, Services.keyManager.getPublicKey());
		Communication connection = connect(authentication, ip, portBegin, portBegin + Math.min(50, numPorts));
		if (connection == null)
		{
			return null;
		}
		connection.send(new WhoIAm());
		connection.send(new OpenConnection(remoteKey, IdkWhereToPutThis.createTestNaunce(authentication, remoteKey)));
		Services.notifications.connectionOpened(connection);
		ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection, authentication);
		synchronized (runnables) { runnables.add(connectionRunnable); }
		Services.connectionThreads.execute(connectionRunnable);
		return authentication.waitForAuthentication() ? connection : null;
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
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect, trying others if available.", ex);
			}
		}
		return null;
	}
	
	public void handleConnection(Socket accepted) throws IOException
	{
		Authenticator authentication = new Authenticator();
		Communication connection = new Communication(authentication, accepted);
		connection.send(new WhoIAm());
		Services.notifications.connectionOpened(connection);
		try
		{
			ConnectionRunnable connectionRunnable = new ConnectionRunnable(connection, authentication);
			synchronized (runnables)
			{
				runnables.add(connectionRunnable);
			}
			connectionRunnable.run();
		}
		finally
		{
			Services.notifications.connectionClosed(connection);
		}
	}
	
	public void closeAll()
	{
		synchronized (runnables) {
			for (ConnectionRunnable r : runnables)
				r.die();
		}
	}
	
	void remove(ConnectionRunnable connectionRunnable)
	{
		synchronized (runnables) {
			runnables.remove(connectionRunnable);
		}
	}
}
