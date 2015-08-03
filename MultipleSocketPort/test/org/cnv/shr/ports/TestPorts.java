package org.cnv.shr.ports;
//
//
//package org.cnv.shr.ports;
//
//import java.util.Scanner;
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class TestPorts
//{
//	static class ReadWrite extends Thread
//	{
//		boolean read;
//		LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue();
//		Window window;
//		
//		ReadWrite(Window window, boolean read)
//		{
//			this.window = window;
//			this.read = read;
//		}
//		
//		void perform(int num)
//		{
//			queue.offer(num);
//		}
//		
//		public void run()
//		{
//			try
//			{
//				for (;;)
//				{
//					int next = queue.take();
//					if (read)
//					{
//						int offset = 0;
//						while (offset < next)
//						{
//							offset += window.read(null, 0, next);
//						}
//					}
//					else
//					{
//						window.write(null, 0, next, -1);
//					}
//				}
//			}
//			catch (InterruptedException e)
//			{
//				e.printStackTrace();
//			}
//		}
//	}
//
//	public static void main(String[] args)
//	{
//		Window window   = new Window(32);
//		ReadWrite read  = new ReadWrite(window, true);
//		ReadWrite write = new ReadWrite(window, false);
//		
//		read.start();
//		write.start();
//
//		try (Scanner scanner = new Scanner(System.in);)
//		{
//			while (scanner.hasNext())
//			{
//				String next = scanner.next();
//				int length = scanner.nextInt();
//
//				if (next.startsWith("r"))
//				{
//					read.perform(length);
//				}
//				else if (next.startsWith("w"))
//				{
//					write.perform(length);
//				}
//				else if (next.startsWith("q"))
//				{
//					break;
//				}
//			}
//		}
//	}
//}
