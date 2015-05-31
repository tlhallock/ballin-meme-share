package org.cnv.shr.track;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Track
{
	static int TRACKER_PORT = 9001;

	public static void main(String[] args) throws IOException
	{
		
		// start trackers
		// start garbage collector
		try (ServerSocket serverSocket = new ServerSocket(TRACKER_PORT);)
		{
			Services.tracker = new Tracker(null);

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
