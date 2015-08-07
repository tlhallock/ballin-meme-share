package org.cnv.shr.ports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import org.cnv.shr.ports.MetaMsg.SocketContext;
import org.cnv.shr.ports.Streams.OutputStreamWrapperIf;

public class SingleConnection implements AutoCloseable
{
	private WindowInputStream  inputStream  = new WindowInputStream (8192);
	private WindowOutputStream outputStream = new WindowOutputStream(8192);

	private DatagramPacket packet;
	private IndexedByteArray array;
	private OutputStreamWrapperIf msgStream;
	
	private SocketContext context;
	
	private Address desitination;

	SingleConnection(OutputStreamWrapperIf output, Address destination, int localId, int remoteId)
	{
		byte[] buf = new byte[MetaMsg.MAXIMUM_MESSAGE_SIZE];
		packet = new DatagramPacket(buf, MetaMsg.MAXIMUM_MESSAGE_SIZE);
		array = new IndexedByteArray(buf);
		context = new SocketContext(localId, remoteId);
		this.msgStream = output;
		this.desitination = destination;
		packet.setSocketAddress(new InetSocketAddress(destination.getIp(), destination.getPort()));
	}
	
	public InputStream getInputStream() throws IOException
	{
		return inputStream;
	}
	public OutputStream getOutputStream() throws IOException
	{
		return outputStream;
	}
	
	public void shutdownOutput() throws IOException
	{
		outputStream.close();
	}
	
	public void shutdownInput(long totalSent) throws IOException
	{
		inputStream.setTotalSent(totalSent);
		inputStream.close();
	}
	
	public synchronized void close() throws IOException
	{
		shutdownInput(0);
		shutdownOutput();
	}
	
	public boolean isClosed() throws IOException
	{
		return inputStream.isClosed() && outputStream.isClosed();
	}
	
	WindowInputStream getWindowIn()
	{
		return inputStream;
	}
	WindowOutputStream getWindowOut()
	{
		return outputStream;
	}
	
	void resendContent()
	{
		
	}
	
	void sendOpen(int localId) throws IOException
	{
		synchronized (outputStream)
		{
			array.reset();
			MetaMsg.writeOpen(context, array);
			msgStream.write(packet);
		}
	}
	
	void sendAmountRead() throws IOException
	{
		synchronized (outputStream)
		{
			array.reset();
			long amountRead = inputStream.getAmountRead();
			MetaMsg.writeAmountRead(context, array, amountRead);
			msgStream.write(packet);
		}
	}

	Address getDestination()
	{
		return desitination;
	}
}
