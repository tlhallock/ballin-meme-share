package org.cnv.shr.ports;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.cnv.shr.ports.MetaMsg.MetaListener;
import org.cnv.shr.ports.MetaMsg.SocketContext;

public class MultipleSocket extends Thread
{
	
	DatagramSocket socket;

	private LinkedBlockingQueue<SingleConnection> openedConnections = new LinkedBlockingQueue<>();
	private HashMap<Integer, SingleConnection> openConnections = new HashMap<>();

	private int nextId;

	private Timer timer = new Timer();
	private ResendTask timerTask = new ResendTask();
	private ListenerImpl listener = new ListenerImpl();
	
	
	public MultipleSocket(int port) throws SocketException
	{
		socket = new DatagramSocket(port);
	}
	
	public SingleConnection accept() throws InterruptedException
	{
		return openedConnections.take();
	}
	
	
	public SingleConnection connect(String ip, int port)
	{
		return null;
	}
	
	private final class ResendTask extends TimerTask
	{
		@Override
		public void run()
		{
			for (SingleConnection single : openConnections.values())
			{
				single.sendAmountRead();
			}
		}
	}

	private final class ListenerImpl implements MetaListener
	{
		@Override
		public void close(SocketContext context, long totalSent) throws IOException
		{
			SingleConnection single = openConnections.get(context.localId);
			if (single == null)
			{
				return;
			}
			single.shutdownInput(totalSent);
		}

		@Override
		public void setAmountRead(SocketContext context, long readOffset) throws IOException
		{
			SingleConnection single = openConnections.get(context.localId);
			if (single == null)
			{
				return;
			}

			single.outputStream.setRead(readOffset);
		}

		@Override
		public void content(IndexedByteArray array, SocketContext context, long startOffset, int length)
		{
			SingleConnection single = openConnections.get(context.localId);
			if (single == null)
			{
				return;
			}
			single.inputStream.write(array.input, array.offset, length, startOffset);
		}

		@Override
		public void open(int remoteId)
		{
			int localId = nextId++;
			SingleConnection single = new SingleConnection();
			openedConnections.offer(single);
			openConnections.put(localId, single);
		}
	}
}
