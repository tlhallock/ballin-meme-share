
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

import org.cnv.shr.dmn.ChecksumManager;
import org.cnv.shr.stng.Settings;


// TODO: java nio
public class ChunkData
{
	public static void read(Chunk chunk, File f, InputStream input) throws IOException, NoSuchAlgorithmException
	{
		final InputStream oInput = input;
//		input = new GZIPInputStream(new InputStream() {
//			@Override
//			public int read() throws IOException
//			{
//				return oInput.read();
//			}} );
		MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		try (RandomAccessFile toWrite = new RandomAccessFile(f, "rw"))
		{
			toWrite.seek(chunk.getBegin());
			int numberOfBytes = (int) chunk.getSize();

			byte[] buffer = new byte[1024];
			int offset = 0;

			while (offset < numberOfBytes)
			{
				int nread = input.read(buffer, 0, Math.min(numberOfBytes - offset, buffer.length));
				if (nread < 0 && offset < numberOfBytes)
				{
					throw new IOException("Hit end of file too early!");
				}
				toWrite.write(buffer, 0, nread);
				digest.update(buffer, 0, nread);
				offset += nread;
			}
		}
		
		String digestToString = ChecksumManager.digestToString(digest);
		if (!digestToString.equals(chunk.getChecksum()))
		{
			throw new IOException("The checksum did not match!");
		}
	}

	public static void write(Chunk chunk, Path f, OutputStream output) throws IOException
	{
//		final OutputStream oOutput = output;
//		output = new GZIPOutputStream(new OutputStream() {
//			@Override
//			public void close() {}
//			@Override
//			public void write(int arg0) throws IOException
//			{
//				output.write(arg0);
//			}});
		
		
		// TODO: Native IO
		try (RandomAccessFile toRead = new RandomAccessFile(f.toFile(), "r"))
		{
			toRead.seek(chunk.getBegin());
			int numberOfBytes = (int) chunk.getSize();

			byte[] buffer = new byte[1024];
			int offset = 0;

			while (offset < numberOfBytes)
			{
				int nread = toRead.read(buffer, 0, Math.min(numberOfBytes - offset, buffer.length));
				if (nread < 0 && offset < numberOfBytes)
				{
					throw new IOException("Hit end of file too early!");
				}
				output.write(buffer, 0, nread);
				offset += nread;
			}
		}
	}
	
	public static String getChecksum(Chunk chunk, Path f) throws NoSuchAlgorithmException, IOException
	{
		MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		try (SeekableByteChannel toRead = Files.newByteChannel(f);)
		{
			toRead.position(chunk.getBegin());
			int numberOfBytes = (int) chunk.getSize();

			ByteBuffer buffer = ByteBuffer.allocate(128);
			int offset = 0;

			while (offset < numberOfBytes)
			{
				int nread = toRead.read(buffer);
				if (nread < 0 && offset < numberOfBytes)
				{
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
		return getChecksum(chunk, f).equals(chunk.getChecksum());
	}
}
