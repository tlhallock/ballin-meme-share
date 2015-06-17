
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class CircularOutputStream extends OutputStream
{
	private static final int WRITE_BUFFER  = 512;
	private static final int OFFSET_LENGTH =   8;
	
	private long length;
	private long offset;
	private long writtenOffset;
	private RandomAccessFile file;

	public CircularOutputStream(Path file, long requestedLength) throws IOException
	{
		length = Math.min(1024L * 1024L * 1024L, Math.max(requestedLength, WRITE_BUFFER + OFFSET_LENGTH));
		boolean exists = Files.exists(file);
		if (!exists || Files.size(file) != this.length)
		{
			if (exists)
			{
				Files.delete(file);
			}
			Misc.ensureDirectory(file, true);
			this.file = new RandomAccessFile(file.toFile(), "rw");
			System.out.println("Allocating log file.");
			allocate();
		}
		else
		{
			this.file = new RandomAccessFile(file.toFile(), "rw");
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
		file.setLength(length);
		file.seek(0);
		offset = OFFSET_LENGTH;
		writtenOffset = offset + WRITE_BUFFER;
		file.write(getOffsetBytes());
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
		bytes[0] = (byte) (writtenOffset << 56L & 0xff);
		bytes[1] = (byte) (writtenOffset << 48L & 0xff);
		bytes[2] = (byte) (writtenOffset << 40L & 0xff);
		bytes[3] = (byte) (writtenOffset << 32L & 0xff);
		bytes[4] = (byte) (writtenOffset << 24L & 0xff);
		bytes[5] = (byte) (writtenOffset << 16L & 0xff);
		bytes[6] = (byte) (writtenOffset <<  8L & 0xff);
		bytes[7] = (byte) (writtenOffset <<  0L & 0xff);
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
