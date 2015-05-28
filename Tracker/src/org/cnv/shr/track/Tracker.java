package org.cnv.shr.track;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Tracker
{
	private static int MAXIMUM_NUM_ENTRIES = 1000;
	private LinkedList<MachineEntry> entries = new LinkedList<>();
	private File trackerFile;
	
	public Tracker(File trackerFile)
	{
		this.trackerFile = trackerFile;
	}

	synchronized void writeEntries(PrintStream ps)
	{
		for (MachineEntry entry : entries)
		{
			entry.print(ps);
		}
	}
	
	synchronized boolean contains(String ip)
	{
		for (MachineEntry entry : entries)
		{
			if (entry.getIp().equals(ip))
			{
				return true;
			}
		}
		return false;
	}

	synchronized void trim()
	{
		Collections.sort(entries);
		while (entries.size() > MAXIMUM_NUM_ENTRIES)
		{
			entries.removeLast();
		}
	}

	synchronized void remove(MachineEntry entry)
	{
		entries.remove(entry);
	}

	void add(MachineEntry take)
	{
		add(take, true);
	}

	void add(MachineEntry take, boolean save)
	{
		synchronized (this)
		{
			entries.add(take);
		}
		if (!save)
		{
			return;
		}
		trim();
		save(null);
	}

	synchronized List<MachineEntry> getEntries()
	{
		return (List<MachineEntry>) entries.clone();
	}

	private void save(File trackerFile)
	{
		try (PrintStream ps = new PrintStream(trackerFile))
		{
			writeEntries(ps);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	void read() throws FileNotFoundException
	{
		try (Scanner scanner = new Scanner(trackerFile))
		{
			int numEntries = scanner.nextInt();
			for (int i = 0; i < numEntries; i++)
			{
				add(new MachineEntry(scanner), false);
			}
		}
		save(trackerFile);
	}
	
	void exchange(String ip, int port)
	{
		try (Socket socket = new Socket(ip, port);
			 PrintStream ps = new PrintStream(socket.getOutputStream());
			 Scanner scanner = new Scanner(socket.getInputStream());)
		{
			ps.print(Track.TRACKER_PORT    ); ps.print(' ');
			ps.print(Track.TRACKER_PORT + 1); ps.print(' ');
			ps.print(String.valueOf(true));   ps.print(' ');
			
			int numEntries = scanner.nextInt();
			for (int i = 0; i < numEntries; i++)
			{
				MachineEntry take = new MachineEntry(scanner);
				
				if (contains(take.getIp()))
				{
					continue;
				}
				Services.pending.add(take);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
