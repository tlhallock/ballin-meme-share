package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.OpenConnection;
import org.cnv.shr.msg.key.WhoIAm;

public class ConnectionManager
{
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
		AuthenticationWaiter authentication = new AuthenticationWaiter(acceptAnyKeys, remoteKey, Services.keyManager.getPublicKey());
		final Communication connection = new Communication(authentication, ip, port);
		connection.send(new WhoIAm());
		connection.send(new OpenConnection(remoteKey, Services.keyManager.createTestNaunce(authentication, remoteKey)));
		Services.notifications.connectionOpened(connection);
		Services.connectionThreads.execute(new ConnectionRunnable(connection, authentication));
		return authentication.waitForAuthentication() ? connection : null;
	}
	
	public synchronized void handleConnection(Socket accepted) throws IOException
	{
		AuthenticationWaiter authentication = new AuthenticationWaiter();
		Communication connection = new Communication(authentication, accepted);
		Services.notifications.connectionOpened(connection);
		connection.send(new WhoIAm());
		Services.connectionThreads.execute(new ConnectionRunnable(connection, authentication));
		Services.notifications.connectionClosed(connection);
	}
}
