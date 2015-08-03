package org.cnv.shr.ports;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.util.zip.CRC32;

public class Streams
{
	public abstract class OutputStreamWrapperIf
	{
		public abstract void write(MetaMsg msg) throws IOException;
	}

	public abstract class InputStreamWrapperIf
	{
		public void read(DatagramPacket packet) throws IOException
		{
			byte[] temporary = new byte[MetaMsg.MAGIC.length];
			while (true)
			{
//				readPastMagic(temporary, null);
				
				long chkSum;
				try
				{
					readInternal(packet);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					continue;
				}
				chkSum = -1;
				if (Streams.getChecksum(packet) != chkSum)
				{
					continue;
				}
			}
		}

		protected abstract void readInternal(DatagramPacket packet) throws IOException;
	}
	
	
	
	
	
	
	

//
//	private static boolean isMagic(byte[] temporary, int offset)
//	{
//		for (int i = 0; i < MetaMsg.MAGIC.length; i++)
//		{
//			if (temporary[offset] != MetaMsg.MAGIC[i])
//			{
//				return false;
//			}
//			offset++;
//			if (offset >= temporary.length)
//			{
//				offset = 0;
//			}
//		}
//		return true;
//	}
//	
//	private static void readPastMagic(byte[] temporary, InputStream input) throws IOException
//	{
//		for (int i = 0; i < temporary.length; i++)
//		{
//			int read = input.read();
//			if (read < 0)
//			{
//				throw new IOException("Read the end of stream");
//			}
//			temporary[i] = (byte) read;
//		}
//		
//		int offset = 0;
//		while (true)
//		{
//			if (isMagic(temporary, offset))
//			{
//				return;
//			}
//			
//			int read = input.read();
//			if (read < 0)
//			{
//				throw new IOException("Read the end of stream");
//			}
//			temporary[offset++] = (byte) read;
//			if (offset >= temporary.length)
//			{
//				offset = 0;
//			}
//		}
//	}
//	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static OutputStreamWrapperIf getOutputStream(OutputStream output)
	{
		return null;
	}
	public static InputStreamWrapperIf getOutputStream(InputStream output)
	{
		return null;
	}
	
	
	

	public static OutputStreamWrapperIf getFaultyOutputStream(OutputStream output)
	{
		return null;
	}
	public static InputStreamWrapperIf getFaultyOutputStream(InputStream output)
	{
		return null;
	}

	public static long getChecksum(DatagramPacket packet)
	{
		CRC32 crc32 = new CRC32();
		crc32.update(packet.getData(), 0, packet.getLength());
		return crc32.getValue();
	}
}
