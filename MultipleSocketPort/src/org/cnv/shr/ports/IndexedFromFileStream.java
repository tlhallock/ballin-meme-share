package org.cnv.shr.ports;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IndexedFromFileStream extends IndexedOutputStream
{
	RandomAccessFile file;
	
	@Override
	public void read(byte[] other, int offset, int length, long startOffset) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(int b) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
}
