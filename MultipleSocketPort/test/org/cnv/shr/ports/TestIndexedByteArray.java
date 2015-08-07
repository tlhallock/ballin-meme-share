package org.cnv.shr.ports;

import java.io.IOException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestIndexedByteArray
{
	private static final Random random = new Random();
	
	private static int BUFFER_SIZE = 32;
	IndexedByteArray array;
	
	@Before
	public void before()
	{
		array = new IndexedByteArray(new byte[BUFFER_SIZE]);
	}

	@Test
	public void testInt() throws IOException
	{
		testInt(random.nextInt());
	}

	@Test
	public void testNegative() throws IOException
	{
		testInt(Integer.MIN_VALUE);
	}
	
	@Test
	public void testPositive() throws IOException
	{
		testInt(Integer.MAX_VALUE);
	}

	@Test
	public void testZero() throws IOException
	{
		testInt(0);
	}

	@Test
	public void testPositiveLong() throws IOException
	{
		testLong(Integer.MAX_VALUE + 1L);
	}
	
	@Test
	public void testNegativeLong() throws IOException
	{
		testLong(-56);
	}

	@Test
	public void testReadPastEnd()
	{
		array.reset(0, 5);
		try
		{
			array.readLong();
			Assert.fail();
		}
		catch (IOException ex)
		{
		}
	}

	@Test
	public void testWritePastEnd()
	{
		array.reset(0, 5);
		try
		{
			array.writeLong(0);
			Assert.fail();
		}
		catch (IOException ex)
		{
		}
	}

	
	private void testInt(int value) throws IOException
	{
		array.reset(0, 4);
		array.writeInt(value);
		array.reset(0, 4);
		int actual = array.readInt();
		Assert.assertEquals(value, actual);
	}

	public void testLong(long value) throws IOException
	{
		array.reset(0, 8);
		array.writeLong(value);
		array.reset(0, 8);
		long actual = array.readLong();
		Assert.assertEquals(value, actual);
	}
}
