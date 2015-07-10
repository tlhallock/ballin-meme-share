
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



package org.cnv.shr.dmn.dwn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.cnv.shr.dmn.ChecksumManager;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;


// TODO: java nio
public class ChunkData
{
	public static boolean read(Chunk chunk, File f, InputStream input) throws IOException, NoSuchAlgorithmException
	{
		MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		try (RandomAccessFile toWrite = new RandomAccessFile(f, "rw"))
		{
			long end    = chunk.getEnd();
			long offset = chunk.getBegin();
			toWrite.seek(offset);

			byte[] buffer = new byte[Misc.BUFFER_SIZE];

			while (offset < end)
			{
				int numToRead = buffer.length;
				long nRem = end - offset;
				if (nRem < numToRead)
				{
					numToRead = (int) nRem;
				}
				int nread = input.read(buffer, 0, numToRead);
				if (nread >= 0)
				{
					LogWrapper.getLogger().info("Read " + (offset - chunk.getBegin()) + " to " + (offset - chunk.getBegin() + nread));
					LogWrapper.getLogger().info(Misc.format(Arrays.copyOfRange(buffer, 0, 10)) + "..." + Misc.format(Arrays.copyOfRange(buffer, nread - 10, nread)));
					
					toWrite.write(buffer, 0, nread);
					digest.update(buffer, 0, nread);
					offset += nread;
					continue;
				}

				if (offset < end)
				{
					throw new IOException("Hit end of file too early!");
				}
			}
		}
		
		String digestToString = ChecksumManager.digestToString(digest);
		return digestToString.equals(chunk.getChecksum());
	}

	public static void write(Chunk chunk, Path f, OutputStream output) throws IOException
	{
		// TODO: Native IO
		try (RandomAccessFile toRead = new RandomAccessFile(f.toFile(), "r"))
		{
			toRead.seek(chunk.getBegin());
			long numberOfBytes = chunk.getSize();

			int bufferSize = Misc.BUFFER_SIZE;
			if (bufferSize > chunk.getSize())
			{
				bufferSize = (int) chunk.getSize();
			}
			byte[] buffer = new byte[bufferSize];
			long offset = 0;

			while (offset < numberOfBytes)
			{
				int nextRead = buffer.length;
				if (numberOfBytes - offset < nextRead)
				{
					nextRead = (int) (numberOfBytes - offset);
				}
				int nread = toRead.read(buffer, 0, nextRead);
				if (nread >= 0)
				{
					LogWrapper.getLogger().info("Sending " + offset + " to " + (offset + nread));
					LogWrapper.getLogger().info(Misc.format(Arrays.copyOfRange(buffer, 0, 10)) + "..." + Misc.format(Arrays.copyOfRange(buffer, nread - 10, nread)));
					output.write(buffer, 0, nread);
					offset += nread;
					continue;
				}
				if (offset < numberOfBytes)
				{
					throw new IOException("Hit end of file too early!");
				}
			}
		}
		
		LogWrapper.getLogger().info("Done serving bytes chunk " + chunk);
		
		output.flush();
	}
	
	public static String getChecksum(Chunk chunk, Path f) throws NoSuchAlgorithmException, IOException
	{
		MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		try (SeekableByteChannel toRead = Files.newByteChannel(f);)
		{
			toRead.position(chunk.getBegin());
			long numberOfBytes = chunk.getSize();

			ByteBuffer buffer = ByteBuffer.allocate(128);
			long offset = 0;

			while (offset < numberOfBytes)
			{
				int nread = toRead.read(buffer);
				if (nread < 0 && offset < numberOfBytes)
				{
					LogWrapper.getLogger().info("Hit end of file too early (at " + offset + ") while calculating checksum of " + chunk);
					return null;
				}
				buffer.flip();
				offset += nread;
				digest.update(buffer);
				buffer.clear();
			}
		}
		
		return ChecksumManager.digestToString(digest);
	}
	
	public static boolean test(Chunk chunk, Path f) throws IOException, NoSuchAlgorithmException
	{
		String checksum = getChecksum(chunk, f);
		if (checksum == null)
		{
			return false;
		}
		return checksum.equals(chunk.getChecksum());
	}
}
