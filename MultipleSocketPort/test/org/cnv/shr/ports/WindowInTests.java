package org.cnv.shr.ports;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WindowInTests
{

	private static Random random = new Random(1776);
	private static int    bufferLength  = 256;
	private static byte[] currentBuffer = new byte[bufferLength];
	private static boolean[] knownBuffer = new boolean[bufferLength];
	private static byte[] outBuffer = new byte[bufferLength];
	private static long currentOffset = 0;

	private static WindowInputStream inStream;

	@Before
	public void before()
	{
		random.nextBytes(currentBuffer);
		for (int i = 0; i < bufferLength; i++)
		{
			knownBuffer[i] = false;
		}
		currentOffset = 0;
		inStream = new WindowInputStream(bufferLength);
	}
	
//	private static void write(int start, int stop)
//	{
//		inStream.write(currentBuffer, start, stop - start, currentOffset + (stop - start));
//		for (int i = start; i < stop; i++)
//		{
//			knownBuffer[i] = true;
//		}
//	}
//
//	private static void read(int amount)
//	{
//		
//	}
//	
//	private static void next()
//	{
//		inStream.write(null, 0, bufferLength, currentOffset);
//		while (inStream.getAmountRead() < currentOffset)
//		{
//			inStream.read(null, 0, (int) (inStream.getAmountRead() - currentOffset));
//		}
//		currentOffset += bufferLength;
//		random.nextBytes(currentBuffer);
//	}
	
	@Test
	public void readTwice()
	{
		int length = 10;
		inStream.write(currentBuffer, 0, length, 0);
		Assert.assertEquals(0, inStream.getAmountRead());
		Assert.assertEquals(length, inStream.available());

		inStream.read(outBuffer, 0, length / 2);
		Assert.assertEquals(length / 2, inStream.getAmountRead());
		Assert.assertEquals(length / 2, inStream.available());

		inStream.read(outBuffer, length / 2, length + 2);
		Assert.assertEquals(length, inStream.getAmountRead());
		Assert.assertEquals(0, inStream.available());

		for (int i = 0; i < length; i++)
		{
			Assert.assertEquals(currentBuffer[i], outBuffer[i]);
		}
	}
	

	@Test
	public void dontUnderWrite()
	{
		inStream = new WindowInputStream(10);
		
		inStream.write(currentBuffer, 0, 20, 0);
		Assert.assertEquals(0, inStream.getAmountRead());
		Assert.assertEquals(10, inStream.available());

		inStream.read(outBuffer, 0, 5);
		Assert.assertEquals(5, inStream.getAmountRead());
		Assert.assertEquals(5, inStream.available());
		for (int i = 0; i < 5; i++)
			Assert.assertEquals(currentBuffer[i], outBuffer[i]);
		
		// write correct data...
		inStream.write(currentBuffer, 0, 50, 0);
		Assert.assertEquals(5, inStream.available());
		
		// write bad data (should not be written)...
		inStream.write(currentBuffer, 5, 5, 0);
		Assert.assertEquals(5, inStream.available());
		
		int currentRead = 5;
		while (currentRead < 15)
		{
			currentRead += inStream.read(outBuffer, currentRead, 15 - currentRead);
		}

		for (int i = 0; i < 15; i++)
			Assert.assertEquals(currentBuffer[i], outBuffer[i]);
		Assert.assertEquals(0, inStream.available());
	}
	

	@Test
	public void testClose()
	{
		inStream = new WindowInputStream(10);
		
		inStream.write(currentBuffer, 0, 10, 0);
		Assert.assertEquals(0, inStream.getAmountRead());
		Assert.assertEquals(10, inStream.available());
		// check available
		
		inStream.setTotalSent(8);
		Assert.assertFalse(inStream.isClosed());
		
		Assert.assertEquals(8, inStream.read(outBuffer, 0, 20));
		Assert.assertEquals(8, inStream.getAmountRead());

		Assert.assertTrue(inStream.isClosed());
		
		// check available
		
		for (int i = 0; i < 8; i++)
			Assert.assertEquals(currentBuffer[i], outBuffer[i]);
		
		Assert.assertEquals(-1, inStream.read(outBuffer, 0, 20));
		Assert.assertTrue(inStream.isClosed());
	}
	
	
	// close
}
