package org.cnv.shr.dmn.mn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.EverythingSoFarInputStream;
import org.cnv.shr.util.Misc;

public class SpeedTest
{
	private static final InputTimer task = new InputTimer();
	private static class InputTimer extends TimerTask
	{
		HashMap<String, CountingInputStream> inputStreams = new HashMap<>();
//		HashMap<String, Long> lastOutput = new HashMap<>();
		
		public void add(String name, CountingInputStream stream)
		{
			inputStreams.put(name,  stream);
//			lastOutput.put(name, (long) 0);
		}
		
		public void run()
		{
			System.out.println("--------------------------------------------------");
			
			long[] longs = new long[inputStreams.size()];
			int count = 0;
			
			for (Entry<String, CountingInputStream> entry : inputStreams.entrySet())
			{
				long current = entry.getValue().getSoFar();
				longs[count++] = current;
//				long last = lastOutput.get(entry.getKey());
//				lastOutput.put(entry.getKey(), current);
				System.out.println(entry.getKey() + ": " + Misc.formatDiskUsage(entry.getValue().getSoFar()));
			}
			
			if (count == 2)
			{
				System.out.println("Ratio: " + (longs[0] / (double) longs[1]));
			}
		}
	}
	
	private static void generateData(int port)
	{
		Random random = new Random();
		byte[] dataToSend = new byte[8192];
		random.nextBytes(dataToSend);
		String str = Misc.format(dataToSend);
		
		try (ServerSocket serverSocket = new ServerSocket(port);
				 Socket socket1 = serverSocket.accept();
				 OutputStream output = socket1.getOutputStream();
				 Writer writer = new OutputStreamWriter(output);)
		{
			for (;;)
			{
				writer.write(str);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void read(InputStreamReader input, int chunkSize) throws IOException
	{
		char[] bytes = new char[chunkSize];
		while (true)
		{
				input.read(bytes, 0, chunkSize);
		}
	}

	private static void createInputStream(String name, int port)
	{
		try (Socket socket = new Socket("127.0.0.1", port);
				 InputStream other = socket.getInputStream();)
		{
			CountingInputStream input = new CountingInputStream(other);
			InputStreamReader reader;
			switch (name)
			{
			case "everything":
				EverythingSoFarInputStream in = new EverythingSoFarInputStream(input, 8192);
				reader = new InputStreamReader(in);
				new Thread(in).start();
				break;
			case "one":
				reader = new InputStreamReader(input)
				{
					@Override
					public int read(char[] cbuf, int offset, int length) throws IOException
					{
						return super.read(cbuf, offset, 1);
					}
				};
				break;
			default:
				return;
			}
			task.add(name, input);
			read(reader, 1024);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException
	{
		new Timer().scheduleAtFixedRate(task, 1000, 1000);
		new Thread(() -> { generateData(5001); }).start();
		new Thread(() -> { generateData(5002); }).start();
		Thread.sleep(1000);
		new Thread(() -> { createInputStream("everything", 5001); }).start();
		new Thread(() -> { createInputStream("one", 5002); }).start();
	}
}
