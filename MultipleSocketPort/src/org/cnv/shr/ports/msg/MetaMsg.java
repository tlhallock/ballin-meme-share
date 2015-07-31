package org.cnv.shr.ports.msg;

public class MetaMsg
{
	private static final int PACKET_SIZE = 1024;
	int crc32;
	int count;
	int length;
	byte[] data;
	int destinationId;
	int type;
	
	
}
