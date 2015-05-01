package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class ConnectionManager
{
	private HashMap<String, Connection> openConnections = new HashMap<>();
	
	public Connection openConnection(String url) throws UnknownHostException, IOException
	{
		int index = url.indexOf(':');
		if (index < 0)
		{
			return openConnection(url, Services.settings.defaultPort);
		}
		else
		{
			return openConnection(url.substring(0, index), Integer.parseInt(url.substring(index + 1, url.length())));
		}
	}
	
	public synchronized Connection openConnection(String ip, int port) throws UnknownHostException, IOException
	{
		Connection connection = new Connection(ip, port);
		openConnections.put(connection.getUrl(), connection);
		run(connection);
		return connection;
	}
	
	public synchronized void handleConnection(Socket accepted) throws IOException
	{
		Connection connection = new Connection(accepted);
		openConnections.put(connection.getUrl(), connection);
		run(connection);
	}
	
	private void run(final Connection c)
	{
		Services.connectionThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				c.run();
				
				synchronized (getThis())
				{
					openConnections.remove(c.getUrl());
				}
			}});
	}

	private ConnectionManager getThis()
	{
		return this;
	}
}
