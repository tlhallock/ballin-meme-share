package org.cnv.shr.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import org.cnv.shr.util.PausableInputStream2;
import org.cnv.shr.util.PausableOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class PausingStreamTests
{
	@Test
	public void testEasySmallBuffer() throws IOException
	{
		byte[] testBuff = "some characters".getBytes();
		try (ByteArrayOutputStream o = new ByteArrayOutputStream();)
		{
			try (PausableOutputStream output = new PausableOutputStream(o);)
			{
				output.write(testBuff);
			}
			int count = 0;
			byte[] otherSide = new byte[testBuff.length];
			try (PausableInputStream2 input = new PausableInputStream2(new ByteArrayInputStream(o.toByteArray()), 5))
			{
				int soFar = 0;
				while (soFar < testBuff.length)
				{
					int read = input.read(otherSide, soFar, testBuff.length - soFar);
					if (read < 0)
					{
						count++;
						input.startAgain();
					}
					else
					{
						soFar += read;
					}
				}
				Assert.assertEquals(-1, input.read());
			}
			Assert.assertArrayEquals(testBuff, otherSide);
			Assert.assertEquals(0, count);
		}
	}
	@Test
	public void testEasyBigBuffer() throws IOException
	{
		byte[] testBuff = "some characters".getBytes();
		try (ByteArrayOutputStream o = new ByteArrayOutputStream();)
		{
			try (PausableOutputStream output = new PausableOutputStream(o);)
			{
				output.write(testBuff);
			}
			int count = 0;
			byte[] otherSide = new byte[testBuff.length];
			try (PausableInputStream2 input = new PausableInputStream2(new ByteArrayInputStream(o.toByteArray()), 500))
			{
				int soFar = 0;
				while (soFar < testBuff.length)
				{
					int read = input.read(otherSide, soFar, testBuff.length - soFar);
					if (read < 0)
					{
						count++;
						input.startAgain();
					}
					else
					{
						soFar += read;
					}
				}
				Assert.assertEquals(-1, input.read());
			}
			Assert.assertArrayEquals(testBuff, otherSide);
			Assert.assertEquals(0, count);
		}
	}
	
	@Test
	public void testEscapeMiddle() throws IOException
	{
		byte[] testIt = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, 3, 4,};
		byte[] output = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE,                                  3, 4,};
		test(testIt, output, 0, 4);
	}
	@Test
	public void testAllEscape() throws IOException
	{
		byte[] testIt = new byte[] { PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, 
																 PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, };
		byte[] output = new byte[] { PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, };
		test(testIt, output, 0, 3);
	}
	@Test
	public void testEscapeEnd() throws IOException
	{
		byte[] testIt = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE};
		byte[] output = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE,                                };
		test(testIt, output, 0, 4);
	}
	@Test
	public void testEscapeBegin() throws IOException
	{
		byte[] testIt = new byte[] { PausableInputStream2.PAUSE_BYTE, PausableInputStream2.PAUSE_BYTE, 1, 2, 3, 4};
		byte[] output = new byte[] { PausableInputStream2.PAUSE_BYTE,                                  1, 2, 3, 4};
		test(testIt, output, 0, 4);
	}
	@Test
	public void testPauseMiddle() throws IOException
	{
		byte[] testIt = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE, 0, 3, 4};
		byte[] output = new byte[] { 1, 2, 3, 4};
		test(testIt, output, 1, 5);
	}
	@Test
	public void testPauseEndBoundaryMiddle() throws IOException
	{
		byte[] testIt = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE, 0, 3, 4};
		byte[] output = new byte[] { 1, 2, 3, 4};
		test(testIt, output, 1, 3);
	}
	@Test
	public void testPauseEndBoundaryEnd() throws IOException
	{
		byte[] testIt = new byte[] { 1, 2, PausableInputStream2.PAUSE_BYTE, 0, 3, 4};
		byte[] output = new byte[] { 1, 2, 3, 4};
		test(testIt, output, 1, 4);
	}
	@Test
	public void testPauseBegin() throws IOException
	{
		byte[] testIt = new byte[] { PausableInputStream2.PAUSE_BYTE, 0, 3, 4};
		byte[] output = new byte[] { 3, 4};
		test(testIt, output, 1, 4);
	}
	@Test
	public void testPauseBeginLarge() throws IOException
	{
		byte[] testIt = new byte[] { PausableInputStream2.PAUSE_BYTE, 0, 3, 4};
		byte[] output = new byte[] { 3, 4};
		test(testIt, output, 1, 500);
	}
	@Test
	public void testSeveralPause() throws IOException
	{
		byte[] testIt = new byte[] { 1, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, PausableInputStream2.PAUSE_BYTE, 0, 2};
		byte[] output = new byte[] { 1, 2 };
		test(testIt, output, 8, 3);
	}

	public void test(byte[] inputArray, byte[] expectedOutput, int expectedRestart, int bufSize) throws IOException
	{
		int count = 0;
		byte[] otherSide = new byte[expectedOutput.length];
		try (PausableInputStream2 input = new PausableInputStream2(new ByteArrayInputStream(inputArray), bufSize))
		{
			int soFar = 0;
			while (soFar < expectedOutput.length)
			{
				int read = input.read(otherSide, soFar, expectedOutput.length - soFar);
				if (read == 0)
				{
					throw new RuntimeException("Returned 0!");
				}
				if (read < 0)
				{
					count++;
					input.startAgain();
					Assert.assertTrue(count <= expectedRestart);
				}
				else
				{
					soFar += read;
				}
			}
			Assert.assertEquals(-1, input.read());
		}
		Assert.assertArrayEquals(expectedOutput, otherSide);
		Assert.assertEquals(expectedRestart, count);
	}

	@Test
	public void testFuzzy() throws IOException
	{
		Random random = new Random();
		int ITERS = 10000;
		for (int i = 0; i < ITERS; i++)
		{
			if (Math.random() < .01)
			{
				System.out.println((i / (double) ITERS) + "%");
			}
			
			int bufSize = 5 + random.nextInt(50);
			byte[] bytes = new byte[50 * bufSize];
			random.nextBytes(bytes);
			
			try (ByteArrayOutputStream output = new ByteArrayOutputStream();)
			{
				int pauseCount = 0;
				try (PausableOutputStream pauseOut = new PausableOutputStream(output);)
				{
					int offset = 0;
					while (offset < bytes.length)
					{
						int nextWrite;
						if (random.nextBoolean())
						{
							nextWrite = 1 + random.nextInt(3 * bufSize);
						}
						else
						{
							nextWrite = 1 + random.nextInt(5);
						}
						if (nextWrite > bytes.length - offset)
						{
							nextWrite = bytes.length - offset;
						}
						pauseOut.write(bytes, offset, nextWrite);
						offset += nextWrite;
						if (random.nextBoolean())
						{
							pauseOut.close();
							if (offset < bytes.length)
							{
								pauseCount++;
							}
						}
					}
				}
				
				byte[] actualOutput = new byte[bytes.length];
				int actualPauseCount = 0;
				try (PausableInputStream2 pauseIn = new PausableInputStream2(new ByteArrayInputStream(output.toByteArray()), bufSize))
				{
					int offset = 0;
					while (offset < bytes.length)
					{
						int nr;
						if (random.nextBoolean())
						{
							nr = Math.max(1, random.nextInt(bytes.length - offset));
						}
						else
						{
							nr = Math.max(1, 10);
						}
						int read = pauseIn.read(actualOutput, offset, nr);
						Assert.assertNotEquals(read, 0);
						if (read < 0)
						{
							actualPauseCount++;
							pauseIn.startAgain();
							Assert.assertTrue(actualPauseCount <= pauseCount);
							continue;
						}
						offset += read;
					}
					Assert.assertEquals(-1, pauseIn.read());
				}

				Assert.assertEquals(pauseCount, actualPauseCount);
				Assert.assertArrayEquals(bytes, actualOutput);
			}
		}
	}
}
