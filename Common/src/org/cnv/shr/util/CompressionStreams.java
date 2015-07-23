//package org.cnv.shr.util;
//
//import java.io.IOException;
//import java.io.OutputStream;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipInputStream;
//import java.util.zip.ZipOutputStream;
//
//import org.cnv.shr.util.HardToCloseStreams.HardToCloseInputStream;
//import org.cnv.shr.util.HardToCloseStreams.HardToCloseOutputStream;
//
//public class CompressionStreams
//{
//	
//	
//	
//	
//	private static ZipEntry createZipEntry(int increment)
//	{
//		ZipEntry zipEntry = new ZipEntry(String.valueOf(increment));
//		zipEntry.setMethod(ZipEntry.DEFLATED);
//		return zipEntry;
//	}
//	
//	private static final class ZipStats
//	{
//		long uncompressed;
//		long compressed;
//		public String toString()
//		{
//			return "compression ratio: " + compressed / (double) uncompressed;
//		}
//	}
//
//	public static HardToCloseOutputStream newCompressedOutputStream(PausableOutputStream delegate) throws IOException
//	{
//		ZipStats stats = new ZipStats();
//		delegate.stopOtherSide();
//		ZipOutputStream zip = new ZipOutputStream(new OutputStream()
//		{
//			int nextName = 1;
//			@Override
//			public void write(int b) throws IOException
//			{
//				delegate.write(b);
//				stats.compressed++;
//			}
//			@Override
//			public void write(byte[] b, int off, int len) throws IOException
//			{
//				delegate.write(b, off, len);
//				stats.compressed+=len;
//			}
//
//			@Override
//			public void close() throws IOException
//			{
//				delegate.close();
//			}
//			@Override
//			public void flush() throws IOException
//			{
//				delegate.flush();
//			}
//		});
//		zip.setLevel(9);
//		zip.putNextEntry(createZipEntry(0));
//		
//		HardToCloseOutputStream outerStream = new HardToCloseOutputStream()
//		{
//			int nextName = 1;
//			@Override
//			public void write(int b) throws IOException
//			{
//				zip.write(b);
//				stats.uncompressed++;
//			}
//			@Override
//			public void write(byte[] b, int off, int len) throws IOException
//			{
//				zip.write(b, off, len);
//				stats.uncompressed+=len;
//				
//				System.out.println("Compressed " + new String(b, off, len));
//			}
//
//			@Override
//			public void close() throws IOException
//			{
//				zip.close();
//				LogWrapper.getLogger().info(stats.toString());
//			}
//			@Override
//			public void flush() throws IOException
//			{
//				zip.putNextEntry(createZipEntry(nextName++));
//				zip.flush();
//				delegate.flush();
//				LogWrapper.getLogger().info(stats.toString());
//				System.out.println("Flushed");
//			}
//			@Override
//			public void actuallyClose() throws IOException
//			{
//				close();
//				delegate.actuallyClose();
//			}
//		};
//		return outerStream;
//	}
//
//	public static HardToCloseInputStream newCompressedInputStream(PausableInputStream2 delegate) throws IOException
//	{
//		delegate.startAgain();
//		ZipInputStream zip = new ZipInputStream(delegate);
//		zip.getNextEntry();
//
//		HardToCloseInputStream outer = new HardToCloseInputStream()
//		{
//			@Override
//			public int available() throws IOException
//			{
//				if (delegate.available() > 0)
//				{
//					System.out.println("delegate available: allot");
//					return Integer.MAX_VALUE;
//				}
//				
//					System.out.println("zip available: 0");
//					return 0;
//				// ZipInputStream doesn't know anything...
////				return delegate.available();
//			}
//			@Override
//			public int read() throws IOException
//			{
//				int read;
//				do
//				{
//					read = zip.read();
//				}
//				while (read < 0 && zip.getNextEntry() != null);
//				return read;
//			}
//			@Override
//			public int read(byte[] buf, int off, int len) throws IOException
//			{
//				int read;
//				do
//				{
//					read = zip.read(buf, off, len);
//					if (read > 0)
//					{
//						System.out.println("Uncompressed: \"" + new String(buf, off, read) + "\"");
//						if (new String(buf, off, read).equals("["))
//						{
//							System.out.println("Read my start array...");
//						}
//					}
//				}
//				while (read < 0 && zip.getNextEntry() != null);
//				return read;
//			}
//			@Override
//			public void close() throws IOException
//			{
//				zip.close();
//				delegate.startAgain();
//			}
//			@Override
//			public void actuallyClose() throws IOException
//			{
//				zip.close();
//				delegate.actuallyClose();
//			}
//		};
//		return outer;
//	}
//}
