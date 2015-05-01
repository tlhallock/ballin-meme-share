package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.cnv.shr.mdl.Machine;

public class ConnectionManager
{
	private HashMap<String, Connection> openConnections = new HashMap<>();
	
	public synchronized Connection openConnection(Machine m) throws UnknownHostException, IOException
	{
		Connection connection = new Connection(m);
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
