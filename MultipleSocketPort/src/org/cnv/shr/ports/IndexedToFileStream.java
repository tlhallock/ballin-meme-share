package org.cnv.shr.ports;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IndexedToFileStream extends IndexedOutputStream
{
	RandomAccessFile file;
	IntervalPersistance written;

	@Override
	public void read(byte[] other, int offset, int length, long startOffset) throws IOException
	{
		
	}

	@Override
	public void write(int b) throws IOException
	{
		
	}
}
