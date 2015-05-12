package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.HashMap;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.InitiateAuthentication;

public class ConnectionManager
{
	private HashMap<String, Communication> openConnections = new HashMap<>();
	
	public Communication openConnection(String url, boolean acceptKeys) throws UnknownHostException, IOException
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			// Should try all other ports too...
			return openConnection(url, Services.settings.servePortBegin.get(), null, acceptKeys);
		}
		else
		{
			return openConnection(url.substring(0, index), Integer.parseInt(url.substring(index + 1, url.length())), null, acceptKeys);
		}
	}
	public Communication openConnection(Machine m, boolean acceptKeys) throws UnknownHostException, IOException
	{
		return openConnection(m.getIp(), m.getPort(), DbKeys.getKey(m), acceptKeys);
	}
	
	private synchronized Communication openConnection(String ip, int port, final PublicKey remoteKey, boolean acceptAnyKeys) throws UnknownHostException, IOException
	{
		final Communication connection = new Communication(ip, port, acceptAnyKeys);
		openConnections.put(connection.getUrl(), connection);

		final byte[] naunceRequest = Services.keyManager.createTestNaunce(connection, remoteKey);
		Services.connectionThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				connection.run();
				
				synchronized (getThis())
				{
					openConnections.remove(connection.getUrl());
				}
				
				connection.setKeys(remoteKey, Services.keyManager.getPublicKey());
				connection.send(new InitiateAuthentication(remoteKey, naunceRequest));
			}});
		if (!connection.waitForAuthentication())
		{
			return null;
		}
		return connection;
	}
	
	public synchronized void handleConnection(Socket accepted) throws IOException
	{
		Communication connection = new Communication(accepted);
		openConnections.put(connection.getUrl(), connection);
		connection.run();
		
		synchronized (getThis())
		{
			openConnections.remove(connection.getUrl());
		}
	}

	private ConnectionManager getThis()
	{
		return this;
	}
}
