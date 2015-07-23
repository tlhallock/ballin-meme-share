package org.cnv.shr.phone.clnt;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.phone.cmn.Appointment;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.cmn.Services;
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

public class Dialer extends Thread implements MsgHandler
{
	private OperatorInfo info;
	private DialListener listener;

	private DialerPersistance persistance;
	
	private Hashtable<String, ConnectListener> listeners;
	
	private long lastHeartBeatSent;
	private long lastHeartBeatReceived;
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	private LinkedList<PhoneMessage> pending = new LinkedList<>();
	boolean hasMore;
	
	public Dialer(
			OperatorInfo info,
			DialListener listener,
			DialerPersistance persistance)
	{
		this.persistance = persistance;
		this.info = info;
		this.listener = listener;
		listeners = new Hashtable<>();
	}

	@Override
	public void run()
	{
		lastHeartBeatSent = -1;
		lastHeartBeatReceived = -1;
		
		while (true)
		{
			TimerTask task = null;
			try (PhoneLine phoneLine = new PhoneLine(new Socket(info.ip, info.beginPort), false);)
			{
				task = new TimerTask()
				{
					public void run()
					{
						requestHeartBeat(phoneLine);
					}
				};
				Services.timer.scheduleAtFixedRate(task, persistance.params.HEARTBEAT_PERIOD, persistance.params.HEARTBEAT_PERIOD);
				phoneLine.sendMessage(new ClientInfo(persistance.params.ident));
				
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
				if (task != null)
					task.cancel();
			}
			

			lock.lock();
			try
			{
					condition.await(getWaitTime(), TimeUnit.MILLISECONDS);
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

	private long getWaitTime()
	{
		synchronized (persistance.appointments)
		{
			if (persistance.appointments.isEmpty())
			{
				return persistance.params.CONNECTION_INACTIVE_REPEAT_MILLISECONDS;
			}
			Appointment appointment = persistance.appointments.get(0);
			while (appointment.isPast())
			{
				persistance.appointments.removeFirst();
				persistance.save();
				if (persistance.appointments.isEmpty())
				{
					return persistance.params.CONNECTION_INACTIVE_REPEAT_MILLISECONDS;
				}
				appointment = persistance.appointments.get(0);
			}
			
			if (appointment.isActive())
			{
				return persistance.params.CONNECTION_ACTIVE_REPEAT_MILLISECONDS;
			}
			return Math.min(persistance.params.CONNECTION_INACTIVE_REPEAT_MILLISECONDS, 
								Math.max(persistance.params.CONNECTION_ACTIVE_REPEAT_MILLISECONDS, 
										appointment.getDate() - System.currentTimeMillis()));
		}
	}

	public void dial(PhoneNumberWildCard number, ConnectListener conListener)
	{
		String uniqueKey = persistance.params.ident + System.currentTimeMillis() + Math.random();
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
	public void onVoicemail(PhoneLine phoneLine, VoiceMail mail)
	{
		if (mail.pleaseReply())
		{
			synchronized (persistance.appointments)
			{
				persistance.appointments.add(new Appointment(mail.getReplyTime(), mail.getSourceNumber().getWildCard()));
				Collections.sort(persistance.appointments);
				persistance.save();
			}
		}
		if (mail.hasData())
		{
			listener.onVoiceMail(mail);
		}
	}

	@Override
	public void onHeartBeatAwk(HeartBeatResponse res)
	{
		lastHeartBeatReceived = System.currentTimeMillis();
	}
	
	private void requestHeartBeat(PhoneLine line)
	{
		if (line == null)
		{
			return;
		}
		lastHeartBeatSent = System.currentTimeMillis();
		line.sendMessage(new HeartBeatRequest(line.params));

		if (lastHeartBeatSent - lastHeartBeatReceived > persistance.params.HEARTBEAT_TIMEOUT)
		{
			System.out.println("Timeout...");
		}
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

	@Override
	public void onMissedCall(PhoneLine line, DialFail dialFail)
	{
		long time = System.currentTimeMillis() + 30 * 60 * 1000;
		line.sendMessage(new VoiceMail(time, dialFail.getNumber()));
		
		synchronized (persistance.appointments)
		{
			persistance.appointments.add(new Appointment(time, dialFail.getNumber()));
			Collections.sort(persistance.appointments);
			persistance.save();
		}
	}
}
