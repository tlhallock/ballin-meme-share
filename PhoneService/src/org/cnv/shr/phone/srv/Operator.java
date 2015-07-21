package org.cnv.shr.phone.srv;

import java.io.IOException;
import java.net.ServerSocket;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.msg.ClientInfo;
import org.cnv.shr.phone.msg.Dial;
import org.cnv.shr.phone.msg.Hangup;
import org.cnv.shr.phone.msg.HeartBeatRequest;
import org.cnv.shr.phone.msg.HeartBeatResponse;
import org.cnv.shr.phone.msg.MsgHandler;
import org.cnv.shr.phone.msg.NoMoreMessages;
import org.cnv.shr.phone.msg.PhoneMessage;
import org.cnv.shr.phone.msg.PhoneRing;
import org.cnv.shr.phone.msg.VoiceMail;

public class Operator implements Runnable, MsgHandler
{
	private PhoneProvider isp;
	private ServerSocket server;
	private VoiceMailManager manager;
	private int port;
	
	private boolean clientHasMore;

	public Operator(PhoneProvider isp, VoiceMailManager manager, int port) throws IOException
	{
		this.isp = isp;
		server = new ServerSocket(port);
		this.manager = manager;
		this.port = port;
	}

	@Override
	public void run()
	{
		Thread.currentThread().setName("operator_" + port);
		outer:
		for (;;)
		{
			ClientInfo info = null;
			try (PhoneLine phoneLine = new PhoneLine(server.accept(), false);)
			{
				clientHasMore = true;
				while (clientHasMore)
				{
					PhoneMessage readMessage = phoneLine.readMessage();
					if (info == null)
					{
						if (!(readMessage instanceof ClientInfo))
						{
							phoneLine.sendMessage(new Hangup(-1, "First message must be client info"));
							continue outer;
						}
						info = (ClientInfo) readMessage;
					}
					readMessage.perform(phoneLine, this);
				}
				
				isp.sendPending(phoneLine);

				isp.unregister(info);
				phoneLine.sendMessage(new NoMoreMessages());
				phoneLine.flush();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				isp.unregister(info);
			}
		}
	}

	@Override
	public void onHeartBeatReq(PhoneLine line, HeartBeatRequest req)
	{
		line.sendMessage(new HeartBeatResponse(req));
	}

	@Override
	public void onDial(PhoneLine line, Dial dial) throws InterruptedException
	{
		isp.handleDial(dial, line);
	}

	@Override
	public void onVoicemail(VoiceMail mail)
	{
		manager.newVoiceMail(null, mail);
	}

	@Override
	public void onNoMore()
	{
		clientHasMore = false;
	}

	@Override
	public void onClientInfo(PhoneLine line, ClientInfo clientInfo)
	{
		line.setInfo(clientInfo);
		isp.register(clientInfo, line);
	}
	
	
	
	
	
	
	


	@Override
	public void onRing(PhoneRing ring)
	{
		// unexpected message...
	}

	@Override
	public void onError(Hangup hangup)
	{
		// unexpected message....
	}

	@Override
	public void onHeartBeatAwk(HeartBeatResponse res)
	{
		// unexpected message...
	}
}
