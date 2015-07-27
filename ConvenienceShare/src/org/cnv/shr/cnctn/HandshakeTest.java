//package org.cnv.shr.cnctn;
//
//import java.io.IOException;
//import java.net.ServerSocket;
//
//import org.cnv.shr.dmn.BlackList;
//import org.cnv.shr.dmn.Services;
//import org.cnv.shr.stng.Settings;
//import org.cnv.shr.util.KeysService;
//import org.cnv.shr.util.NonRejectingExecutor;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//public class HandshakeTest
//{
//	static ConnectionParams params = new ConnectionParams()
//	{
//		@Override
//		protected void opened(Communication connection) throws Exception
//		{
//			System.out.println("We get here...");
//		}
//
//		@Override
//		protected boolean closeWhenDone()
//		{
//			return true;
//		}
//		
//		@Override
//		boolean tryingToConnectToLocal()
//		{
//			return false;
//		}
//	};
//	
//	
//	@BeforeClass
//	public static void setup()
//	{
//		Services.settings = new Settings(null);
//		Services.keyManager = new KeysService();
//		Services.networkManager = new ConnectionManager();
//		Services.blackList = new BlackList() {
//			public void read() {}
//			public void write() {}
//		};
//		Services.connectionThreads =  new NonRejectingExecutor("cnctns", 20);
//
//		params.acceptAllKeys =  false;
//		params.identifier = null;
//		params.ip = "127.0.0.1";
//		params.port = 8090;
//		params.reason = "test";
//	}
//	
//	
//	private static void client() throws InterruptedException
//	{
//		Thread.sleep(1000);
//		Services.networkManager.openConnection(params);
//	}
//	
//	private static void server() throws IOException
//	{
//		ServerSocket serverSocket = new ServerSocket (8090);
//		HandshakeServer handshakeServer = new HandshakeServer(serverSocket, 8091, 8099, testPersistance);
//		handshakeServer.handleConnections();
//	}
//	
//	
//	@Test
//	public void testConnect() throws IOException
//	{
//		Services.connectionThreads.execute(() -> {
//			try
//			{
//				client();
//			}
//			catch (Exception e)
//			{
//				Assert.fail();
//			}
//		});
//		server();
//	}
//}
