package org.cnv.shr.ports;

import java.io.IOException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WindowOutTests
{
	private static final Random random = new Random();
	private static final int bufferSize = 32;
	private static final byte[] testBytes = new byte[20 * bufferSize];
	private static final byte[] outBytes = new byte[20 * bufferSize];
	private static WindowOutputStream outputStream;
	
	@Before
	public void beforeTest()
	{
		random.nextBytes(testBytes);
		outputStream = new WindowOutputStream(bufferSize);
	}
	
	
	private static void writeQuickly(int maxStepSize)
	{
		new Thread(() -> {
			int offset = 0;
			while (offset < testBytes.length)
			{
				int nextWrite = random.nextInt(maxStepSize);
				outputStream.write(testBytes, offset, nextWrite);
				offset += nextWrite;
			}
		}).start();
	}
	
	@Test
	public void oneTest() throws IOException
	{
		int mid = bufferSize / 2;
		outputStream.write(testBytes, 0, mid);
		outputStream.write(testBytes, mid, bufferSize - mid);
		
		int start, stop;
		
		start = 10;
		stop  = 15;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		

		start = 0;
		stop  = 15;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		

		start = 10;
		stop  = 32;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		

		start = 10;
		stop  = 15;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		
		start = 0;
		stop  = bufferSize;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=0;i<bufferSize;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
	}
	

	@Test
	public void cantReadBack() throws IOException
	{
		int mid = bufferSize / 2;
		outputStream.write(testBytes, 0, mid);
		outputStream.setRead(2);
		outputStream.write(testBytes, mid, bufferSize - mid + 2);
		
		int start, stop;
		
		start = 10;
		stop  = 15;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		

		start = 0;
		stop  = 15;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		

		start = 10;
		stop  = 32;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		

		start = 10;
		stop  = 15;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=start;i<stop;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		
		start = 2;
		stop  = bufferSize + 2;
		outputStream.read(outBytes, start, stop - start, start);
		for (int i=0;i<bufferSize;i++) Assert.assertEquals(testBytes[i], outBytes[i]);
		
		try
		{
			outputStream.read(outBytes, start, 0, 5);
			Assert.fail();
		}
		catch (Exception e)
		{
			System.out.println("Excepted exception caught");
			e.printStackTrace();
		}
	}
}

