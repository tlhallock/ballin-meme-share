package org.cnv.shr.ports;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.zip.CRC32;

class Streams
{
	public static final int CRC_SIZE = 4;
	
	public static abstract class OutputStreamWrapperIf
	{
		public void write(DatagramPacket msg) throws IOException
		{
			long chksum = Streams.getChecksum(msg);
			msg.getData()[0] = (byte)((chksum >>  0) & 0xff);
			msg.getData()[1] = (byte)((chksum >>  8) & 0xff);
			msg.getData()[2] = (byte)((chksum >> 16) & 0xff);
			msg.getData()[3] = (byte)((chksum >> 24) & 0xff);
		}
		
		protected abstract void writeInternal(DatagramPacket packet) throws IOException;
	}

	public static abstract class InputStreamWrapperIf
	{
		public void read(DatagramPacket packet) throws IOException
		{
//			byte[] temporary = new byte[MetaMsg.MAGIC.length];
			while (true)
			{
//				readPastMagic(temporary, null);
				
				try
				{
					readInternal(packet);
				}
				catch (IOException e)
				{
					e.printStackTrace();
					continue;
				}
				

				int chkSum = 0;
				chkSum |= (packet.getData()[0] & 0xff) <<  0;
				chkSum |= (packet.getData()[1] & 0xff) <<  8;
				chkSum |= (packet.getData()[2] & 0xff) << 16;
				chkSum |= (packet.getData()[3] & 0xff) << 24;
				
				if (Streams.getChecksum(packet) != chkSum)
				{
					System.out.println("Packet drop");
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

	public static OutputStreamWrapperIf getOutputStream(DatagramSocket output)
	{
		return new OutputStreamWrapperIf()
		{
			@Override
			protected void writeInternal(DatagramPacket packet) throws IOException
			{
				output.send(packet);
			}
		};
	}
	public static InputStreamWrapperIf getInputStream(DatagramSocket output)
	{
		return new InputStreamWrapperIf()
		{
			@Override
			protected void readInternal(DatagramPacket packet) throws IOException
			{
				output.receive(packet);
			}
		};
	}
	
	
	

	public static OutputStreamWrapperIf getFaultyOutputStream(DatagramSocket output)
	{
		return null;
	}
	public static InputStreamWrapperIf getFaultyInputStream(DatagramSocket output)
	{
		return null;
	}

	public static long getChecksum(DatagramPacket packet)
	{
		CRC32 crc32 = new CRC32();
		crc32.update(packet.getData(), CRC_SIZE, packet.getLength() - CRC_SIZE);
		return crc32.getValue();
	}
}
