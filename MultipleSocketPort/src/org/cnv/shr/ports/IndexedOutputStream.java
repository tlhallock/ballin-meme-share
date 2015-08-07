package org.cnv.shr.ports;

import java.io.IOException;
import java.io.OutputStream;

public abstract class IndexedOutputStream extends OutputStream
{
	public abstract void read(byte[] other, int offset, int length, long startOffset) throws IOException;
	
	public static final class Interval
	{
		long start;
		long stop;

		public Interval(long b, long e)
		{
			this.start = b;
			this.stop = e;
		}
	};
}
