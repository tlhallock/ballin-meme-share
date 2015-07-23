package org.cnv.shr.phone.srv;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.cnv.shr.phone.cmn.Appointment;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumber;
import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.cmn.Services;
import org.cnv.shr.phone.msg.ClientInfo;
import org.cnv.shr.phone.msg.Dial;
import org.cnv.shr.phone.msg.DialFail;
import org.cnv.shr.phone.msg.PhoneRing;
import org.cnv.shr.phone.msg.VoiceMail;

public class PhoneProvider
{
	private static final long KEEP_OPEN_ON_ACTIVE = 5 * 60 * 1000;
	
	
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();
	
	
	private PortStatus[] portStatus;
	private VoiceMailManager manager;
	
	private Hashtable<ClientInfo, PhoneLine> openLines = new Hashtable<>();
	
	private LinkedList<AppointmentEntry> appointments = new LinkedList<>();

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
	
	public void sendPending(PhoneLine line) throws IOException, InterruptedException
	{
		manager.sendVoiceMails(line);
		
		PhoneNumber number = line.getInfo().getNumber();
		
		if (isActive(number))
		{
			long releaseTime = System.currentTimeMillis() + KEEP_OPEN_ON_ACTIVE;
			do
			{
				lock.lock();
				try
				{
					// send new voice mails
					condition.awaitUntil(new Date(releaseTime));
				}
				finally
				{
					lock.unlock();
				}
				
				
			} while (System.currentTimeMillis() < releaseTime);
		}
		
		line.flush();
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
		if (other == null)
		{
			line.sendMessage(new DialFail(dial.getNumber()));
			return;
		}
		
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
	
	public Appointment getNextAppointment(PhoneNumber info)
	{
		synchronized (appointments)
		{
			for (AppointmentEntry entry : appointments)
			{
				if (entry.appointmentFor.matches(info))
				{
					return entry.appointment;
				}
			}
		}
		return null;
	}
	
	public boolean isActive(PhoneNumber info)
	{
		Appointment nextAppointment = getNextAppointment(info);
		return nextAppointment != null && nextAppointment.isActive();
	}
	
	public void addAppointment(PhoneNumberWildCard appointmentFor, Appointment appointment)
	{
		synchronized (appointments)
		{
			appointments.add(new AppointmentEntry(appointmentFor, appointment));
			Collections.sort(appointments);
		}
	}

	private static final class AppointmentEntry implements Comparable<AppointmentEntry>
	{
		private PhoneNumberWildCard appointmentFor;
		private Appointment appointment;
		
		public AppointmentEntry(PhoneNumberWildCard appointmentFor, Appointment appointment)
		{
			this.appointmentFor = appointmentFor;
			this.appointment = appointment;
		}

		@Override
		public int compareTo(AppointmentEntry o)
		{
			int c1 = appointment.compareTo(o.appointment);
			if (c1 != 0) return c1;
			return appointmentFor.compareTo(o.appointmentFor);
		}
	}

	public void onVoiceMail(PhoneLine phoneLine, VoiceMail mail)
	{
		manager.newVoiceMail(null, mail);
		if (mail.pleaseReply())
		{
//			PhoneNumberWildCard destination = mail.getDestinationNumber();
//			if (!destination.hasNumber())
//			{
//				phoneLine.sendMessage(new Hangup(-1, "You must request a precise phone number to leave a voicemail."));
//				return;
//			}
			
			addAppointment(mail.getDestinationNumber(),                   new Appointment(mail.getReplyTime(), mail.getSourceNumber().getWildCard()));
			addAppointment(phoneLine.getInfo().getNumber().getWildCard(), new Appointment(mail.getReplyTime(), mail.getDestinationNumber()));
		}
	}
	
}
