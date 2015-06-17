
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

import java.util.LinkedList;

public final class ByteListBuffer extends AbstractByteWriter
{
	int length;
	private LinkedList<byte[]> bytesSoFar = new LinkedList<>();

	private static final int BUFFER_LENGTH = 256;
	private byte[] currentBuffer = new byte[BUFFER_LENGTH];
	private int offset;

	@Override
	public ByteListBuffer append(byte[] bytes)
	{
		length += bytes.length;
		if (offset + bytes.length < currentBuffer.length)
		{
			System.arraycopy(bytes, 0, currentBuffer, offset, bytes.length);
			offset += bytes.length;
			return this;
		}
		
		for (int i = 0; i < bytes.length; i++)
		{
			checkEnd();
			currentBuffer[offset++] = bytes[i];
		}
		return this;
	}
	
	@Override
	public ByteListBuffer append(byte i)
	{
		checkEnd();
		currentBuffer[offset++] = i;
		length++;
		return this;
	}

	public byte[] getBytes()
	{
		byte[] allBytes = new byte[length];
		int currentOffset = 0;
		for (byte[] cBytes : bytesSoFar)
		{
			System.arraycopy(cBytes, 0, allBytes, currentOffset, cBytes.length);
			currentOffset += cBytes.length;
		}
		System.arraycopy(currentBuffer, 0, allBytes, currentOffset, offset);
		
		bytesSoFar.clear();
		bytesSoFar.add(allBytes);
		offset = 0;

		return allBytes;
	}
	
	private void checkEnd()
	{
		if (offset < currentBuffer.length)
		{
			return;
		}
		bytesSoFar.add(currentBuffer);
		currentBuffer = new byte[BUFFER_LENGTH];
		offset = 0;
	}
	
	@Override
	public long getLength()
	{
		return length;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

//	@Override
//	public ByteListBuffer append(boolean hasFile)
//	{
//		try
//		{
//			return (ByteListBuffer) super.append(hasFile);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//			return this;
//		}
//	}
//
//	@Override
//	public ByteListBuffer append(String str)
//	{
//		try
//		{
//			return (ByteListBuffer) super.append(str);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//			return this;
//		}
//	}
//
//	@Override
//	public ByteListBuffer appendVarByteArray(byte[] bytes)
//	{
//		try
//		{
//			return (ByteListBuffer) super.appendVarByteArray(bytes);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//			return this;
//		}
//	}
//
//	@Override
//	public ByteListBuffer append(PublicKey key)
//	{
//		try
//		{
//			return (ByteListBuffer) super.append(key);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//			return this;
//		}
//	}
//
//	@Override
//	public ByteListBuffer append(SharedFileId key)
//	{
//		try
//		{
//			return (ByteListBuffer) super.append(key);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//			return this;
//		}
//	}
//
//	@Override
//	public ByteListBuffer append(double percentComplete)
//	{
//		try
//		{
//			return (ByteListBuffer) super.append(percentComplete);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, , e);
//			return this;
//		}
//	}
}
