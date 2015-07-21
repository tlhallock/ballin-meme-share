package org.cnv.shr.phone.srv;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.cnv.shr.phone.cmn.PhoneLine;

public class Relay implements Runnable
{
	private PhoneLine from;
	private PhoneLine to;
	private CountDownLatch latch;
	
	private byte[] buffer = new byte[8192];
	
	public Relay(PhoneLine from, PhoneLine to, CountDownLatch latch)
	{
		this.from = from;
		this.to = to;
		this.latch = latch;
	}
	
	@Override
	public void run()
	{
		Thread.currentThread().setName("relay_" + Math.random());
		try
		{
			while (true)
			{
				int read = from.input.read(buffer, 0, buffer.length);
				if (read < 0)
				{
					to.socket.shutdownOutput();
				}
				to.output.write(buffer, 0, read);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			latch.countDown();
		}
	}
}
