package org.cnv.shr.dmn.dwn;

import java.util.HashMap;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;

public class Seeder
{
	Machine machine;
	int numRequests;
	int numSuccess;
	double bps;
	HashMap<Long, ChunkRequest> pending = new HashMap<>();
	Communication connection;
	
	Seeder(Machine machine, Communication openConnection)
	{
		this.machine = machine;
		this.connection = openConnection;
	}
	
	public ChunkRequest request(Chunk removeFirst)
	{
		ChunkRequest msg = new ChunkRequest(removeFirst);
		pending.put(System.currentTimeMillis(), msg);
		connection.send(msg);
		return msg;
	}

	public void send(Message message)
	{
		connection.send(message);
	}

	public void done()
	{
		connection.send(new DoneMessage());
	}

}