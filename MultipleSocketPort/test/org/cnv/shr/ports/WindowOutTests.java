package org.cnv.shr.ports;

import java.util.Random;

import org.cnv.shr.ports.WindowOutputStream;
import org.junit.Before;
import org.junit.Test;

public class WindowOutTests
{
	private static final Random random = new Random();
	private static final int bufferSize = 32;
	private static final byte[] testBytes = new byte[20 * bufferSize];
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
	public void oneTest()
	{
	}
}

