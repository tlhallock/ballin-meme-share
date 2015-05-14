package org.cnv.shr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.NoSuchPaddingException;

public class TestTest
{
	public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException
	{
		String localIp = InetAddress.getLocalHost().getHostAddress();
		int port1 = 7990;
		int port2 = 8990;
		
		MachineInfo machine1 = new MachineInfo("bin" + File.separator + "instance1", port1, "clOvFbFlEEDh0sfCQieGwbwCv2E6T9sBarPnJ0UV52zb8XDn1f");
		MachineInfo machine2 = new MachineInfo("bin" + File.separator + "instance2", port2, "KroGnSns2whGXu5ihtxlgjdr8Xg4YxuJjS5oKsq2DS8NeLn046");
		
		try (ServerSocket socket1 = machine1.launch();
		     ServerSocket socket2 = machine2.launch();
			 PrintStream cmd1 = new PrintStream(socket1.accept().getOutputStream());
		     PrintStream cmd2 = new PrintStream(socket2.accept().getOutputStream());)
		{
			Thread.sleep(1000);
			
			
			TestActions.ADD_MACHINE.send(cmd1, localIp + ":" + port2);

			Thread.sleep(10000);
		}
		finally
		{
			machine1.kill();
			machine2.kill();
		}
	}
}
