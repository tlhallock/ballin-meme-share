package org.cnv.shr.trck;

import java.io.PrintStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Scanner;


public class MachineEntry
{
	private String ident;
	private PublicKey key;
	
	private String ip;
	private int beginPort;
	private int endPort;
	

	MachineEntry(String ident, PublicKey key, String ip, int begin, int end)
	{
		this.ident = ident;
		this.key = key;
		this.ip = ip;
		this.beginPort = begin;
		this.endPort = end;
	}
	
	MachineEntry(Socket socket, Scanner scanner)
	{
		this(scanner);
		ip = socket.getInetAddress().getHostName();
	}
	
	public MachineEntry(Scanner scanner)
	{
		ip = scanner.next();
		beginPort = scanner.nextInt();
		endPort = scanner.nextInt();
	}
	
	void print(PrintStream ps)
	{
		ps.print(ip                        ); ps.print(' ');
		ps.print(beginPort                 ); ps.print(' ');
		ps.print(endPort                   ); ps.print('\n');
	}

	public String getIp()
	{
		return ip;
	}

	public int getPort()
	{
		return beginPort;
	}
}
