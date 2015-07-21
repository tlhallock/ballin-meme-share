package org.cnv.shr.phone.clnt;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.cmn.Services;
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

public class Dialer extends Thread implements MsgHandler
{
//	private static final long HEARTBEAT_PERIOD  = 10 * 60 * 1000;
//	private static final long HEARTBEAT_TIMEOUT = Long.MAX_VALUE;
	private static final long CONNECTION_REPEAT_MINUTES = 10;
	
	private OperatorInfo info;
	private DialListener listener;
	
	private Hashtable<String, ConnectListener> listeners;
	
//	private long lastHeartBeatSent;
//	private long lastHeartBeatReceived;
	
	private String ident;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private LinkedList<PhoneMessage> pending = new LinkedList<>();
	boolean hasMore;
	
	public Dialer(
			String ident,
			OperatorInfo info,
			DialListener listener,
			Path voiceMailDir)
	{
		this.ident = ident;
		this.info = info;
		this.listener = listener;
		listeners = new Hashtable<>();
	}

	@Override
	public void run()
	{
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				requestHeartBeat();
			}
		};

//		lastHeartBeatSent = -1;
//		lastHeartBeatReceived = -1;
		
		while (true)
		{
			try (PhoneLine phoneLine = new PhoneLine(new Socket(info.ip, info.beginPort), false);)
			{
//				Services.timer.scheduleAtFixedRate(task, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD);
				phoneLine.sendMessage(new ClientInfo(ident));
				
				List<PhoneMessage> cloned;
				synchronized (pending)
				{
					cloned = (List<PhoneMessage>) pending.clone();
					pending.clear();
				}
				
				for (PhoneMessage msg : cloned)
				{
					phoneLine.sendMessage(msg);
				}
				
				phoneLine.sendMessage(new NoMoreMessages());

				hasMore = true;
				while (hasMore)
				{
					phoneLine.flush();
					phoneLine.readMessage().perform(phoneLine, this);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				listener.onFail(-1, e.getMessage());
			}
			finally
			{
				task.cancel();
			}
			

			lock.lock();
			try
			{
					condition.await(CONNECTION_REPEAT_MINUTES, TimeUnit.MINUTES);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			finally
			{
				lock.unlock();
			}
		}
	}

	public void dial(PhoneNumberWildCard number, ConnectListener conListener)
	{
		String uniqueKey = ident + System.currentTimeMillis() + Math.random();
		if (conListener != null)
		{
			listeners.put(uniqueKey, conListener);
		}
		synchronized (pending)
		{
			pending.add(new Dial(uniqueKey, number));
		}
		
		lock.lock();
		try
		{
			condition.signalAll();
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public void onRing(PhoneRing ring)
	{
		Services.executor.execute(() -> {
			try (PhoneLine replyline = new PhoneLine(new Socket(info.ip, ring.getReplyPort()), true);)
			{
				ConnectListener remove = listeners.remove(ring.getKey());
				if (remove != null)
				{
					remove.onConnect(replyline);
				}
				else
				{
					listener.onRing(ring, replyline);
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		});
	}

	@Override
	public void onError(Hangup hangup)
	{
		listener.onFail(hangup.code, hangup.message);
	}

	@Override
	public void onVoicemail(VoiceMail mail)
	{
		if (mail.pleaseReply())
		{
			dial(mail.getReplyNumber(), null);
		}
		if (mail.hasData())
		{
			listener.onVoiceMail(mail);
		}
	}

	@Override
	public void onHeartBeatAwk(HeartBeatResponse res)
	{
//		lastHeartBeatReceived = System.currentTimeMillis();
	}
	
	private void requestHeartBeat()
	{
//		if (line == null)
//		{
//			return;
//		}
//		lastHeartBeatSent = System.currentTimeMillis();
//		line.sendMessage(new HeartBeatRequest(line.params));
//
//		if (lastHeartBeatSent - lastHeartBeatReceived > HEARTBEAT_TIMEOUT)
//		{
//			System.out.println("Timeout...");
//		}
	}

	@Override
	public void onNoMore()
	{
		hasMore = false;
	}

	@Override
	public void onHeartBeatReq(PhoneLine line, HeartBeatRequest req)
	{
		// Unexpected message...
	}
	@Override
	public void onDial(PhoneLine line, Dial dial)
	{
		// Unexpected message...
	}

	@Override
	public void onClientInfo(PhoneLine line, ClientInfo clientInfo)
	{
		// Unexpected message...
	}
}
