package org.cnv.shr.track;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class MachineEntry implements Comparable<MachineEntry>
{
	private long added;
	private long verified;
	private String ip;
	private int beginPort;
	private int endPort;
	private boolean tracker;
	
	MachineEntry(Socket socket, int begin, int end, boolean tracker)
	{
		this.added = System.currentTimeMillis();
		this.verified = -1;
		ip = socket.getInetAddress().getHostName();
		this.beginPort = begin;
		this.endPort = end;
		this.tracker = tracker;
	}
	
	public MachineEntry(Scanner scanner)
	{
		added = scanner.nextLong();
		verified = scanner.nextLong();
		ip = scanner.next();
		beginPort = scanner.nextInt();
		endPort = scanner.nextInt();
		tracker = scanner.nextBoolean();
	}
	
	void print(PrintStream ps)
	{
		ps.print(added                     ); ps.print(' ');
		ps.print(verified                  ); ps.print(' ');
		ps.print(ip                        ); ps.print(' ');
		ps.print(beginPort                 ); ps.print(' ');
		ps.print(endPort                   ); ps.print(' ');
		ps.print(tracker                   ); ps.print('\n');
	}

	public boolean isTracker()
	{
		return tracker;
	}
	public String getIp()
	{
		return ip;
	}

	public int getPort()
	{
		return beginPort;
	}
	
	boolean checkConnectivity(long now)
	{
		boolean returnValue = false;
		
		outer:
		for (int port = beginPort; port < endPort; port++)
		{
			try (Socket socket = new Socket(ip, port);)
			{
				returnValue = true;
				break outer;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		if (returnValue)
		{
			verified = now;
		}
		return returnValue;
	}

	@Override
	public int compareTo(MachineEntry o)
	{
		int returnValue = Long.compare(verified, o.verified);
		if (returnValue != 0)
		{
			return returnValue;
		}
		return Long.compare(added, o.added);
	}
}