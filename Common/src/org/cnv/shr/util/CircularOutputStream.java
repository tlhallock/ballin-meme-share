package org.cnv.shr.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class CircularOutputStream extends OutputStream
{
	private static final int WRITE_BUFFER  = 512;
	private static final int OFFSET_LENGTH =   8;
	
	private long length;
	private long offset;
	private long writtenOffset;
	private RandomAccessFile file;

	public CircularOutputStream(File file, long requestedLength) throws IOException
	{
		length = Math.min(1024L * 1024L * 1024L, Math.max(requestedLength, WRITE_BUFFER + OFFSET_LENGTH));
		if (file.length() != this.length)
		{
			file.delete();
			Misc.ensureDirectory(file, true);
			System.out.println(file.getAbsolutePath());
			this.file = new RandomAccessFile(file, "rw");
			System.out.println("Allocating log file.");
			allocate();
		}
		else
		{
			this.file = new RandomAccessFile(file, "rw");
			offset = readOffset();
			writtenOffset = getNextOffset();
			writeWrittenOffset();
			checkLoop();
		}
	}

	@Override
	public void write(int b) throws IOException
	{
		checkLoop();
		offset++;
		file.write(b);
	}
	
	private void allocate() throws IOException
	{
		file.seek(0);
		offset = OFFSET_LENGTH;
		writtenOffset = offset + WRITE_BUFFER;
		file.write(getOffsetBytes());
		for (long i = offset; i < length; i++)
		{
			file.write('\n');
		}
		file.seek(offset);
	}
	
	private void checkLoop() throws IOException
	{
		if (checkWriteOffset())
		{
			return;
		}
		if (offset + 1 < length)
		{
			return;
		}
		offset = OFFSET_LENGTH;
		writtenOffset = getNextOffset();
		file.seek(0);
		file.write(getOffsetBytes());
	}
	
	private boolean checkWriteOffset() throws IOException
	{
		if (offset + 1 < writtenOffset)
		{
			return true;
		}
		writtenOffset = getNextOffset();
		writeWrittenOffset();
		return false;
	}

	private long getNextOffset()
	{
		return Math.min(offset + WRITE_BUFFER, length);
	}

	private void writeWrittenOffset() throws IOException
	{
		file.seek(0);
		file.write(getOffsetBytes());
		file.seek(offset);
	}
	
	private byte[] getOffsetBytes() throws IOException
	{
		byte[] bytes = new byte[OFFSET_LENGTH];
		bytes[0] = (byte) ((writtenOffset << 56L) & 0xff);
		bytes[1] = (byte) ((writtenOffset << 48L) & 0xff);
		bytes[2] = (byte) ((writtenOffset << 40L) & 0xff);
		bytes[3] = (byte) ((writtenOffset << 32L) & 0xff);
		bytes[4] = (byte) ((writtenOffset << 24L) & 0xff);
		bytes[5] = (byte) ((writtenOffset << 16L) & 0xff);
		bytes[6] = (byte) ((writtenOffset <<  8L) & 0xff);
		bytes[7] = (byte) ((writtenOffset <<  0L) & 0xff);
		return bytes;
	}
	
	private long readOffset() throws IOException
	{
		long offset = 0;
		offset |= (file.read() & 0xff) << 56L;
		offset |= (file.read() & 0xff) << 48L;
		offset |= (file.read() & 0xff) << 40L;
		offset |= (file.read() & 0xff) << 32L;
		offset |= (file.read() & 0xff) << 24L;
		offset |= (file.read() & 0xff) << 16L;
		offset |= (file.read() & 0xff) <<  8L;
		offset |= (file.read() & 0xff) <<  0L;
		return Math.max(OFFSET_LENGTH, Math.min(length, offset));
	}
	
	@Override
	public void close() throws IOException
	{
		writtenOffset = offset;
		writeWrittenOffset();
		file.close();
	}
}
