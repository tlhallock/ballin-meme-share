package org.cnv.shr.ports;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.ports.MetaMsg.MetaListener;
import org.cnv.shr.ports.MetaMsg.SocketContext;
import org.cnv.shr.ports.SocketParams.DefaultSocketParams;
import org.cnv.shr.ports.Streams.OutputStreamWrapperIf;

public class MultipleSocket implements AutoCloseable
{
	private static final long RESEND_PERIOD = 1000;
	
	private Random random = new Random();
	private DatagramSocket socket;

	private LinkedBlockingQueue<SingleConnection> openedConnections = new LinkedBlockingQueue<>();
	private HashMap<Integer, SingleConnection>    openConnections   = new HashMap<>();
	
	private HashMap<Integer, Address> tryingToOpen = new HashMap<>();

	private Timer timer = new Timer();
	private ResendTask timerTask = new ResendTask();
	private ListenerImpl listener = new ListenerImpl();
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();

	private DatagramPacket output;
	private IndexedByteArray array;
	private OutputStreamWrapperIf msgStream;
	
	private MultipleSocketRunnable reader;
	private Resender resender;

	private boolean closed;

	public MultipleSocket(int port) throws SocketException
	{
		this(new DefaultSocketParams(), port);
	}
	
	public MultipleSocket(SocketParams params, int port) throws SocketException
	{
		socket = new DatagramSocket(port);
		byte[] buf = new byte[MetaMsg.MAXIMUM_MESSAGE_SIZE];
		output = new DatagramPacket(buf, MetaMsg.MAXIMUM_MESSAGE_SIZE);
		msgStream = Streams.getOutputStream(socket);
		(reader = new MultipleSocketRunnable(listener, Streams.getInputStream(socket))).start();
		(resender = new Resender()).start();
		timer.scheduleAtFixedRate(timerTask, RESEND_PERIOD, RESEND_PERIOD);
	}
	
	public SingleConnection accept() throws InterruptedException, IOException
	{
		while (true)
		{
			if (closed) throw new IOException("Unable to accept: closed");
			
			SingleConnection take = openedConnections.take();
			
			if (take == null)
			{
				continue;
			}
			
			return take;
		}
	}

	public SingleConnection connect(String ip, int port) throws IOException
	{
		return connect(new Address(ip, port));
	}
	SingleConnection connect(Address address) throws IOException
	{
		if (closed) return null;
		
		int localId;
		synchronized (openConnections)
		{
			do
			{
				localId = random.nextInt();
			} while (openConnections.containsKey(localId) || tryingToOpen.containsKey(localId));

			tryingToOpen.put(localId, address);
		}

		sendOpen(address, localId, -1);
		
		SingleConnection connection = null;

		lock.lock();
		try
		{
			connection = openConnections.get(localId);
			while (connection == null)
			{
				condition.await(10, TimeUnit.MINUTES);
				connection = openConnections.get(localId);
				if (closed) throw new IOException("Unable to connect: " + closed);
			}
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
			throw new IOException("Unable to connect", e);
		}
		finally
		{
			lock.unlock();
		}
		
		if (connection == null)
		{
			throw new IOException("Unable to connect");
		}

		return connection;
	}

	private final class Resender extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				if (closed) return;

				// send all writes...
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	private final class ResendTask extends TimerTask
	{
		@Override
		public void run()
		{
			if (closed) return;
			for (SingleConnection single : openConnections.values())
			{
				try
				{
					single.sendAmountRead();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private final class ListenerImpl implements MetaListener
	{
		@Override
		public void close(SocketContext context, long totalSent) throws IOException
		{
			if (closed) return;
			SingleConnection single = openConnections.get(context.localId);
			if (single == null)
			{
				return;
			}
			if (!single.getDestination().equals(context.getOrigin()))
			{
				System.out.println("destination mismatch");
				return;
			}
			single.shutdownInput(totalSent);
		}

		@Override
		public void setAmountRead(SocketContext context, long readOffset) throws IOException
		{
			if (closed) return;
			SingleConnection single = openConnections.get(context.localId);
			if (single == null)
			{
				return;
			}
			if (!single.getDestination().equals(context.getOrigin()))
			{
				System.out.println("destination mismatch");
				return;
			}
			single.getWindowOut().setRead(readOffset);
		}

		@Override
		public void content(SocketContext context, IndexedByteArray array, long startOffset, int length)
		{
			if (closed) return;
			SingleConnection single = openConnections.get(context.localId);
			if (single == null)
			{
				return;
			}
			if (!single.getDestination().equals(context.getOrigin()))
			{
				System.out.println("destination mismatch");
				return;
			}
			single.getWindowIn().write(array.getBytes(), array.getOffset(), length, startOffset);
		}

		@Override
		public void open(SocketContext context) throws IOException
		{
			if (closed) return;
			boolean outgoing;
			int localId;
			if (context.localId < 0)
			{
				do
				{
					localId = random.nextInt();
				} while (openConnections.containsKey(localId) || tryingToOpen.containsKey(localId));
				outgoing = false;
			}
			else
			{
				localId = context.localId;

				Address actual = tryingToOpen.get(localId);
				if (!actual.equals(context.getOrigin()))
				{
					// fail
					System.out.println("From different address");
				}
				outgoing = true;
			}
			
			
			SingleConnection single = new SingleConnection(msgStream, context.getOrigin(), localId, context.remoteId);
			synchronized (openConnections)
			{
				tryingToOpen.remove(localId);
				openConnections.put(localId, single);
			}

			if (outgoing)
			{
				lock.lock();
				try
				{
					condition.signalAll();
				}
				finally
				{
					lock.unlock();
				}
			}
			else
			{
				sendOpen(context.getOrigin(), localId, context.remoteId);
				openedConnections.offer(single);
			}
		}
	}

	@Override
	public void close() throws Exception
	{
		// close all the connections
		
		closed = true;
		
		for (SingleConnection connection : openConnections.values())
		{
			connection.close();
		}
		openedConnections.clear();
		tryingToOpen.clear();
		
		resender.interrupt();
		reader.interrupt();
		timer.cancel();
	}
	
	private void sendOpen(Address address, int localId, int remoteId) throws IOException
	{
		synchronized (output)
		{
			array.reset();
			MetaMsg.writeOpen(new SocketContext(localId, remoteId), array);
			output.setSocketAddress(new InetSocketAddress(address.getIp(), address.getPort()));
			msgStream.write(output);
		}
	}
}
