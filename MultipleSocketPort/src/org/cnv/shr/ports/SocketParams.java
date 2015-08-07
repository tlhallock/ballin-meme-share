package org.cnv.shr.ports;

import java.net.DatagramSocket;

import org.cnv.shr.ports.Streams.InputStreamWrapperIf;
import org.cnv.shr.ports.Streams.OutputStreamWrapperIf;

public abstract class SocketParams
{
	abstract OutputStreamWrapperIf createOutputStream(DatagramSocket socket);
	abstract InputStreamWrapperIf createPutputStream(DatagramSocket socket);
	abstract int getBufferSize();
	abstract boolean verifyCheckSums();

	public static class DefaultSocketParams extends SocketParams
	{
		@Override
		OutputStreamWrapperIf createOutputStream(DatagramSocket socket)
		{
			return Streams.getOutputStream(socket);
		}

		@Override
		InputStreamWrapperIf createPutputStream(DatagramSocket socket)
		{
			return Streams.getInputStream(socket);
		}

		@Override
		int getBufferSize()
		{
			return 8192;
		}

		@Override
		boolean verifyCheckSums()
		{
			return true;
		}
	}
	
	public static class TestSocketParams extends SocketParams
	{
		@Override
		OutputStreamWrapperIf createOutputStream(DatagramSocket socket)
		{
			return Streams.getFaultyOutputStream(socket);
		}

		@Override
		InputStreamWrapperIf createPutputStream(DatagramSocket socket)
		{
			return Streams.getFaultyInputStream(socket);
		}

		@Override
		int getBufferSize()
		{
			return 8192;
		}

		@Override
		boolean verifyCheckSums()
		{
			return true;
		}
	}
}
