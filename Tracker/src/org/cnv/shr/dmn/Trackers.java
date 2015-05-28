//package org.cnv.shr.dmn;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.PrintStream;
//import java.net.Socket;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Scanner;
//import java.util.Set;
//
//import org.cnv.shr.gui.AddMachine.AddMachineParams;
//import org.cnv.shr.gui.UserActions;
//import org.cnv.shr.track.MachineEntry;
//
//public class Trackers
//{
//	private File trackerFile;
//	private Set<TrackerAddress> trackerList = Collections.synchronizedSet(new HashSet<TrackerAddress>());
//	
//	public Trackers(File trackerFile)
//	{
//		this.trackerFile = trackerFile;
//	}
//	
//	public void add(String ip, int port)
//	{
//		TrackerAddress e = new TrackerAddress(ip, port);
//		synchronized (trackerList)
//		{
//			trackerList.add(e);
//		}
//		save();
//		synchronize(e);
//	}
//	
//	public void read()
//	{
//		try (Scanner scanner = new Scanner(new FileInputStream(trackerFile)))
//		{
//			while (scanner.hasNext())
//			{
//				final TrackerAddress e = new TrackerAddress(scanner);
//				trackerList.add(e);
//			}
//		}
//		catch (FileNotFoundException e)
//		{
//			e.printStackTrace();
//		}
//	}
//	
//	public void synchronizeAll()
//	{
//		for (TrackerAddress address : trackerList)
//		{
//			synchronize(address);
//		}
//	}
//	
//	private void save()
//	{
//		try (PrintStream ps = new PrintStream(trackerFile))
//		{
//			synchronized (trackerList)
//			{
//				for (TrackerAddress url : trackerList)
//				{
//					url.write(ps);
//				}
//			}
//		}
//		catch (FileNotFoundException e)
//		{
//			e.printStackTrace();
//		}
//	}
//
//	void synchronize(TrackerAddress address)
//	{
//		try (Socket socket = new Socket(address.ip, address.port);
//			 PrintStream ps = new PrintStream(socket.getOutputStream());
//			 Scanner scanner = new Scanner(socket.getInputStream());)
//		{
//			ps.print(Services.settings.servePortBeginE); ps.print(' ');
//			ps.print(Services.settings.maxServes);       ps.print(' ');
//			ps.print(String.valueOf(false));             ps.print(' ');
//			
//			int numEntries = scanner.nextInt();
//			for (int i = 0; i < numEntries; i++)
//			{
//				MachineEntry take = new MachineEntry(scanner);
//				final TrackerAddress trackerAddress = new TrackerAddress(take.getIp(), take.getPort());
//				if (!take.isTracker())
//				{
//					UserActions.addMachine(trackerAddress.getUrl(), new AddMachineParams(false));
//					continue;
//				}
//
//				if (!trackerList.add(trackerAddress))
//				{
//					continue;
//				}
//				
//				Services.userThreads.execute(new Runnable() {
//					@Override
//					public void run()
//					{
//						synchronize(trackerAddress);
//					}});
//				
//				ps.println(0);
//			}
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//	}
//	
//	private static final class TrackerAddress
//	{
//		private String ip;
//		private int port;
//		
//		TrackerAddress(String ip, int port)
//		{
//			this.ip = ip;
//			this.port = port;
//		}
//		
//		String getUrl()
//		{
//			return ip + ":" + port;
//		}
//
//		TrackerAddress(Scanner scanner)
//		{
//			this.ip = scanner.next();
//			this.port = scanner.nextInt();
//		}
//		
//		public void write(PrintStream ps)
//		{
//			ps.println(ip + " " + port);
//		}
//		
//		public int hashCode()
//		{
//			return getUrl().hashCode();
//		}
//		
//		public boolean equals(Object o)
//		{
//			return (o instanceof TrackerAddress) 
//					&& ((TrackerAddress) o).ip.equals(ip)
//					&& ((TrackerAddress) o).port == port;
//		}
//	}
//}
