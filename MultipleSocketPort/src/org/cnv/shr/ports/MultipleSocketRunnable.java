package org.cnv.shr.ports;

import java.io.IOException;
import java.net.DatagramPacket;

import org.cnv.shr.ports.MetaMsg.MetaListener;
import org.cnv.shr.ports.Streams.InputStreamWrapperIf;

class MultipleSocketRunnable extends Thread
{
	private MetaListener listener;
	private InputStreamWrapperIf msgStream;

	MultipleSocketRunnable(MetaListener listener, InputStreamWrapperIf msgStream)
	{
		this.listener = listener;
		this.msgStream = msgStream;
	}

	public void run()
	{
		byte[] buf = new byte[MetaMsg.MAXIMUM_MESSAGE_SIZE];
		DatagramPacket packet = new DatagramPacket(buf, MetaMsg.MAXIMUM_MESSAGE_SIZE);
		IndexedByteArray array = new IndexedByteArray(buf); 

		while (true)
		{
			try
			{
				msgStream.read(packet);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				continue;
			}
			
			array.reset(MetaMsg.MESSAGE_START, packet.getLength());
			String ip = packet.getAddress().getHostAddress();
			int port = packet.getPort();
			Address address = new Address(ip, port);
			
			MetaMsg.handleMessage(listener, array, address);
		}
	}
}
