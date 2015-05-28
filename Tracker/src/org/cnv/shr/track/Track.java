package org.cnv.shr.track;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;

public class Track
{

	static int TRACKER_PORT = 9001;
	private static String PERSISTANT_FILE = "machines.txt";
	private static long VERIFIER_REPEAT = 8 * 60 * 60 * 1000;

	public static void main(String[] args) throws IOException
	{
		try (ServerSocket serverSocket = new ServerSocket(TRACKER_PORT);)
		{
			Services.tracker = new Tracker(new File(PERSISTANT_FILE));
			Services.pending = new PendingRunner();
			Services.tracker.read();
			Services.verifier = new VerifierRunnable();
			
			new Timer().scheduleAtFixedRate(Services.verifier, VERIFIER_REPEAT, VERIFIER_REPEAT);

			for (;;)
			{
				try (Socket connection = serverSocket.accept();
	  				 Scanner scanner = new Scanner(connection.getInputStream());)
				{
					int begin = scanner.nextInt();
					int end = scanner.nextInt();
					boolean isTracker = scanner.nextBoolean();
					if (!portsAreSane(begin, end))
					{
						continue;
					}

					PrintStream ps = new PrintStream(connection.getOutputStream());
					Services.tracker.writeEntries(ps);

					MachineEntry entry = new MachineEntry(connection, begin, end, isTracker);
					Services.pending.add(entry);
					
					scanner.next();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private static boolean portsAreSane(int begin, int end)
	{
		return begin < end && begin > 0 && end < 100000;
	}
}
