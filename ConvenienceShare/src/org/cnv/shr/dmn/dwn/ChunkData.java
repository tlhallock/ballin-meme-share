package org.cnv.shr.dmn.dwn;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.cnv.shr.dmn.ChecksumManager;
import org.cnv.shr.stng.Settings;


public class ChunkData
{
	public static void read(Chunk chunk, File f, InputStream input) throws IOException, NoSuchAlgorithmException
	{
//		final InputStream oInput = input;
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

	public static void write(Chunk chunk, File f, OutputStream output) throws IOException
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
		try (RandomAccessFile toRead = new RandomAccessFile(f, "r"))
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
	
	public static String getChecksum(Chunk chunk, File f) throws NoSuchAlgorithmException, IOException
	{
		MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		try (RandomAccessFile toRead = new RandomAccessFile(f, "r"))
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
					return null;
				}
				offset += nread;
				digest.update(buffer, 0, nread);
			}
		}
		
		return ChecksumManager.digestToString(digest);
	}
	
	public static boolean test(Chunk chunk, File f) throws IOException, NoSuchAlgorithmException
	{
		return getChecksum(chunk, f).equals(chunk.getChecksum());
	}
}
