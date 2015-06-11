package org.cnv.shr.dmn.dwn;

import java.io.IOException;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.trck.FileEntry;

public class Seeder
{
	Machine machine;
	int numRequests;
	int numSuccess;
	double bps;
	// Right now, only one download per peer...
	Communication connection;
	

	long lastRequest;
	
	
	Seeder(Machine machine, Communication openConnection)
	{
		this.machine = machine;
		this.connection = openConnection;
	}
	
	public ChunkRequest request(FileEntry descriptor, Chunk removeFirst) throws IOException
	{
		ChunkRequest msg = new ChunkRequest(descriptor, removeFirst);
		lastRequest = System.currentTimeMillis();
		connection.send(msg);
		return msg;
	}

	public void requestCompleted(FileEntry descriptor, Chunk requested)
	{
	}
	
	public void send(Message message) throws IOException
	{
		connection.send(message);
	}

	public void done()
	{
		connection.finish();
	}

	public Communication getConnection()
	{
		return connection;
	}

	public boolean is(Machine remote)
	{
		return remote.getId() == machine.getId();
	}

	public boolean is(Communication c)
	{
		return connection.equals(c);
	}
}