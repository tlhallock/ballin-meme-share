package org.cnv.shr.phone.cmn;

import java.util.concurrent.CountDownLatch;


public class Signal
{
	private CountDownLatch latch;
	SignalListener listener;
	
	public Signal(SignalListener listener, int count)
	{
		this.listener = listener;
		this.latch = new CountDownLatch(count);
	}
	
	public void done()
	{
		latch.countDown();
		if (latch.getCount() == 0)
		{
			Services.executor.execute(() -> { listener.closed(); } );
		}
	}
	
	
	
	public void waitFor()
	{
		
	}
	
	public interface SignalListener
	{
		public void closed();
	}
}
