package org.cnv.shr.cnctn;


public class HandshakeTest
{
	
	
	private static void client()
	{
		
	}
	
	private static void server()
	{
//		HandshakeServer handshakeServer = new HandshakeServer(8090, 8099);
//		handshakeServer./
		
	}
	
	
	
	public static void main(String[] args)
	{
		new Thread(() -> { server(); } );
		client();
	}
}
