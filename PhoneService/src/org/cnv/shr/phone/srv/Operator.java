package org.cnv.shr.phone.srv;

import java.io.IOException;
import java.net.ServerSocket;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.msg.ClientInfo;
import org.cnv.shr.phone.msg.Dial;
import org.cnv.shr.phone.msg.DialFail;
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
	private PhoneProvider provider;
	private ServerSocket server;
	private int port;
	
	private boolean clientHasMore;

	public Operator(PhoneProvider isp, int port) throws IOException
	{
		this.provider = isp;
		server = new ServerSocket(port);
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
				
				provider.sendPending(phoneLine);

				provider.unregister(info);
				phoneLine.sendMessage(new NoMoreMessages());
				phoneLine.flush();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				provider.unregister(info);
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
		provider.handleDial(dial, line);
	}

	@Override
	public void onVoicemail(PhoneLine phoneLine, VoiceMail mail)
	{
		provider.onVoiceMail(phoneLine, mail);
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
		provider.register(clientInfo, line);
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

	@Override
	public void onMissedCall(PhoneLine line, DialFail dialFail)
	{
		// unexpected message...
	}
}
