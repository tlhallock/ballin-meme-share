package org.cnv.shr.phone.srv;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.Services;
import org.cnv.shr.phone.msg.ClientInfo;
import org.cnv.shr.phone.msg.Dial;
import org.cnv.shr.phone.msg.PhoneRing;

public class PhoneProvider
{
	private PortStatus[] portStatus;

	private VoiceMailManager manager;
	
	private Hashtable<ClientInfo, PhoneLine> openLines = new Hashtable<>();

	public PhoneProvider(VoiceMailManager manager, int begin, int end)
	{
		this.manager = manager;
		portStatus = new PortStatus[end - begin];
		
		for (int port = begin; port < end; port++)
		{
			portStatus[port - begin] = new PortStatus();
			portStatus[port - begin].port = port;
		}
	}
	
	public void register(ClientInfo info, PhoneLine line)
	{
		openLines.put(info, line);
	}

	public void unregister(ClientInfo info)
	{
		if (info == null) return;
		openLines.remove(info);
	}
	
	public void sendPending(PhoneLine line)
	{
		
	}
	
	private PortStatus allocatePort()
	{
		for (PortStatus status : portStatus)
		{
			synchronized (status)
			{
				if (status.inUse)
				{
					continue;
				}
				status.inUse = true;
				return status;
			}
		}
		return null;
	}
	
	private static class PortStatus
	{
		int port;
		boolean inUse;
	}

	private PhoneLine findLine(Dial dial)
	{
		return null;
	}
	
	public void handleDial(Dial dial, PhoneLine line) throws InterruptedException
	{
		CountDownLatch sent = new CountDownLatch(1);

		PhoneLine other = findLine(dial);
		
		Services.executor.execute(() -> {
			
			// allocate the ports
			PortStatus port1 = allocatePort(); if (port1 == null) System.out.println("Uh oh");
			PortStatus port2 = allocatePort(); if (port2 == null) System.out.println("Uh oh");
			
			try (ServerSocket from    = new ServerSocket(port1.port);
					 ServerSocket to      = new ServerSocket(port2.port);)
			{
				line. sendMessage(new PhoneRing(dial.getKey(), port1.port, line .getInfo().getIdent(),  line.params));
				other.sendMessage(new PhoneRing(dial.getKey(), port2.port, other.getInfo().getIdent(),  other.params));
				sent.countDown();
				
				try (PhoneLine    fromLine = new PhoneLine(from.accept(), true);
						 PhoneLine    toLine   = new PhoneLine(to.accept(),   true);)
				{
						CountDownLatch latch = new CountDownLatch(2);
						Relay r1 = new Relay(line, other, latch);
						Relay r2 = new Relay(other, line, latch);
						
						Services.executor.execute(r2);
						r1.run();
						
						try
						{
							latch.await();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
			finally
			{
				if (port1 != null) port1.inUse = false;
				if (port2 != null) port2.inUse = false;
			}
		});
		
		sent.await();
	}
}
