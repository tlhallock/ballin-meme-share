package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.HashMap;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.InitiateAuthentication;
import org.cnv.shr.util.Misc;

public class ConnectionManager
{
	private HashMap<String, Communication> openConnections = new HashMap<>();
	
	public Communication openConnection(String url) throws UnknownHostException, IOException
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			// Should try all other ports too...
			return openConnection(url, Services.settings.servePortBegin.get(), null);
		}
		else
		{
			return openConnection(url.substring(0, index), Integer.parseInt(url.substring(index + 1, url.length())), null);
		}
	}
	public Communication openConnection(Machine m) throws UnknownHostException, IOException
	{
		return openConnection(m.getIp(), m.getPort(), DbKeys.getKey(m));
	}
	
	private synchronized Communication openConnection(String ip, int port, final PublicKey knownKey) throws UnknownHostException, IOException
	{
		final Communication connection = new Communication(ip, port);
		openConnections.put(connection.getUrl(), connection);

		final byte[] original = Misc.createNaunce();
		final byte[] sentNaunce = Services.keyManager.createNaunce(knownKey, original);
		Services.connectionThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				connection.run();
				
				synchronized (getThis())
				{
					openConnections.remove(connection.getUrl());
				}
				
//				PublicKey knownKey = Services.keyManager.getPublicKey(); 
				// If there is a naunce, get it here
				connection.addPendingNaunce(original);
				connection.send(new InitiateAuthentication(knownKey, sentNaunce));
			}});
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
