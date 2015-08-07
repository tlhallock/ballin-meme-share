package org.cnv.shr.ports;

import java.io.IOException;

class MetaMsg
{
	public static final int MAXIMUM_MESSAGE_SIZE = 1024;
	public static final int MESSAGE_START = Streams.CRC_SIZE;
	
	private static final int PACKET_SIZE = 1024;
	public static final byte[] MAGIC = "magic".getBytes();

	public static final int OPEN_CONNECTION = 0;
	public static final int    CONTENT_TYPE = 1;
	public static final int    COUNTER_TYPE = 2;
	public static final int        END_TYPE = 3;
	
	public static interface MetaListener
	{
		void open(SocketContext context) throws IOException;
		void content(SocketContext context, IndexedByteArray array, long startOffset, int length);
		void setAmountRead(SocketContext context, long readOffset) throws IOException;
		void close(SocketContext context, long totalSent) throws IOException;
	}

	public static void handleMessage(MetaListener listener, IndexedByteArray array, Address address)
	{
		try
		{
			int type = array.readInt();
			
			switch (type)
			{
			case MetaMsg.OPEN_CONNECTION:
				parseOpenMessage(listener, array, address);
				break;
			case MetaMsg.CONTENT_TYPE:
				parserContent(listener, array, address);
				break;
			case MetaMsg.COUNTER_TYPE:
				parseAmountRead(listener, array, address);
				break;
			case MetaMsg.END_TYPE:
				parseClose(listener, array, address);
				break;
			default:
				System.out.println("Bad type: " + type);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void writeClose(SocketContext context, IndexedByteArray array) throws IOException
	{
		array.writeInt(END_TYPE);
		// have to write the context somewhere...
	}
	
	private static void parseClose(MetaListener multipleSocket, IndexedByteArray array, Address address) throws IOException
	{
		SocketContext context = new SocketContext(array);
		context.origin = address;
		long totalSent = array.readLong();
		multipleSocket.close(context, totalSent);
	}

	public static void writeAmountRead(SocketContext context, IndexedByteArray array, long amountRead) throws IOException
	{
		
	}
	private static void parseAmountRead(MetaListener multipleSocket, IndexedByteArray array, Address address) throws IOException
	{
		SocketContext context = new SocketContext(array);
		context.origin = address;
		long readOffset = array.readLong();
		multipleSocket.setAmountRead(context, readOffset);
	}

	public static void writeContent() throws IOException
	{
		
	}
	private static void parserContent(MetaListener multipleSocket, IndexedByteArray array, Address address) throws IOException 
	{
		SocketContext context = new SocketContext(array);
		context.origin = address;
		long startOffset = array.readLong();
		int length = array.readInt();
		multipleSocket.content(context, array, startOffset, length);
	}

	public static void writeOpen(SocketContext context, IndexedByteArray array) throws IOException
	{
		
	}
	private static void parseOpenMessage(MetaListener multipleSocket, IndexedByteArray array, Address address) throws IOException
	{
		SocketContext context = new SocketContext(array);
		context.origin = address;
		
		// something about to file
		multipleSocket.open(context);
	}

	public static final class SocketContext
	{
		public final int localId;
		public final int remoteId;
		
		private Address origin;
		
		public SocketContext(IndexedByteArray array) throws IOException
		{
			localId = array.readInt();
			remoteId = array.readInt();
		}

		public SocketContext(int localId, int remoteId)
		{
			this.localId = localId;
			this.remoteId = remoteId;
		}
		
		public Address getOrigin()
		{
			return origin;
		}
	}
}
