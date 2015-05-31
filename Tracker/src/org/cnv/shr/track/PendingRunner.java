//package org.cnv.shr.track;
//
//import java.util.concurrent.LinkedBlockingDeque;
//
//public class PendingRunner implements Runnable
//{
//	private LinkedBlockingDeque<MachineEntry> pending = new LinkedBlockingDeque<>();
//	
//	PendingRunner() {}
//
//	public void add(MachineEntry entry)
//	{
//		if (!contains(entry.getIp()))
//		{
//			pending.add(entry);
//		}
//	}
//
//	synchronized boolean contains(String ip)
//	{
//		for (MachineEntry entry : pending)
//		{
//			if (entry.getIp().equals(ip))
//			{
//				return true;
//			}
//		}
//		return false;
//	}
//	
//	@Override
//	public void run()
//	{
//		for (;;)
//		{
//			MachineEntry take;
//			try
//			{
//				take = pending.take();
//			}
//			catch (InterruptedException e)
//			{
//				e.printStackTrace();
//				continue;
//			}
//			if (take.checkConnectivity(System.currentTimeMillis()))
//			{
//				Services.tracker.add(take);
//			}
//		}
//	}
//}
