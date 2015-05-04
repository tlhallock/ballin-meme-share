package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class ConnectionManager
{
	private HashMap<String, Communication> openConnections = new HashMap<>();
	
	public Communication openConnection(String url) throws UnknownHostException, IOException
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			// Should try all other ports too...
			return openConnection(url, Services.settings.servePortBegin.get());
		}
		else
		{
			return openConnection(url.substring(0, index), Integer.parseInt(url.substring(index + 1, url.length())));
		}
	}
	
	public synchronized Communication openConnection(String ip, int port) throws UnknownHostException, IOException
	{
		final Communication connection = new Communication(ip, port);
		openConnections.put(connection.getUrl(), connection);
		Services.connectionThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				connection.run();
				
				synchronized (getThis())
				{
					openConnections.remove(connection.getUrl());
				}
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
