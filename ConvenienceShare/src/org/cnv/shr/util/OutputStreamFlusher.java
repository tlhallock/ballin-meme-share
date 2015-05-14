package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.EmptyMessage;

public class OutputStreamFlusher extends OutputStream
{
	private boolean written;
	private OutputStream inner;
	private Communication outer;
	
	public OutputStreamFlusher(Communication outer, OutputStream inner)
	{
		this.outer = outer;
		this.inner = inner;
	}
	
	public void flushPending()
	{
		written = false;
		EmptyMessage flusher = new EmptyMessage(DoneMessage.DONE_PADDING);
		while (!written)
		{
			outer.send(flusher);
		}
	}
	
	@Override
	public void write(int arg0) throws IOException
	{
		inner.write(arg0);
		written = true;
	}
}
