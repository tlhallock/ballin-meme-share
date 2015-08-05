package org.cnv.shr.ports;

import java.util.NoSuchElementException;
import java.util.Random;

import org.cnv.shr.ports.IntervalPersistance.WrittenInterval;
import org.junit.Assert;
import org.junit.Test;

public class IntervalTests
{
	@Test
	public void mostSimpleTest()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);
		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		
		persistance.add(5, 7);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(8, 10);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(1, 5);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 1);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 7), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		
		persistance.remove(4);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(4, 7), persistance.getNextBlock());
		Assert.assertEquals(4, persistance.start());
		
		persistance.remove(3);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(7, persistance.start());
		
		persistance.add(7, 8);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(7, 10), persistance.getNextBlock());
		Assert.assertEquals(7, persistance.start());

		persistance.remove(3);
		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(10, persistance.start());
	}
	

	@Test
	public void testCoalesceRight()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(5, 7);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		

		persistance.add(6, 10);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}

	@Test
	public void testCoalesce()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(5, 7);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		

		persistance.add(8, 10);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());


		persistance.add(5, 10);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}

	@Test
	public void testCoalesceSmaller()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(5, 7);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		

		persistance.add(8, 10);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());


		persistance.add(2, 9);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}

	@Test
	public void testCoalesceBigger()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(5, 7);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		

		persistance.add(8, 10);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());


		persistance.add(2, 11);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}

	@Test
	public void testCoalesceMutlipleButNotAll()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 3);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 3), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(5, 7);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 3), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		
		persistance.add(9, 10);
		System.out.println(persistance);
		Assert.assertEquals(3, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 3), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		
		persistance.add(13, 15);
		System.out.println(persistance);
		Assert.assertEquals(4, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 3), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		

		persistance.add(0, 11);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 11), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}

	@Test
	public void testWriteAll()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 18);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 18), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.remove(9);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(9, 18), persistance.getNextBlock());
		Assert.assertEquals(9, persistance.start());

		persistance.remove(9);
		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(18, persistance.start());
	}

	@Test
	public void testDontRead()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 18);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 18), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		try
		{
			persistance.remove(20);
			Assert.fail();
		}
		catch (NoSuchElementException e)
		{
			System.out.println("expected exception thrown");
		}
	}

	@Test
	public void testWrap()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 32);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 32), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
		
		persistance.remove(31);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(31, 32), persistance.getNextBlock());
		Assert.assertEquals(31, persistance.start());
		
		persistance.add(0, 10);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(31, 32), persistance.getNextBlock());
		Assert.assertEquals(31, persistance.start());

		persistance.remove(persistance.getNextBlock().length());
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 10), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}

	@Test
	public void testUnderWrite()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 5);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 5), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(2, 4);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 5), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());
	}
	

	@Test
	public void testBreakOnLeft()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 10);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 10), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.remove(5);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(5, 10), persistance.getNextBlock());
		Assert.assertEquals(5, persistance.start());

		persistance.remove(2);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(7, 10), persistance.getNextBlock());
		Assert.assertEquals(7, persistance.start());
	}

	@Test
	public void testBreakOnRight()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 5);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 5), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.remove(3);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(3, 5), persistance.getNextBlock());
		Assert.assertEquals(3, persistance.start());

		persistance.add(0, 1);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(3, 5), persistance.getNextBlock());
		Assert.assertEquals(3, persistance.start());

		persistance.remove(2);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(5, persistance.start());

		persistance.add(0, 7);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(5, 7), persistance.getNextBlock());
		Assert.assertEquals(5, persistance.start());

		persistance.remove(2);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(7, persistance.start());
	}

	@Test
	public void testBreakOnMiddle()
	{
		IntervalPersistance persistance = new IntervalPersistance(32);

		System.out.println(persistance);
		Assert.assertEquals(0, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.add(0, 5);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(0, 5), persistance.getNextBlock());
		Assert.assertEquals(0, persistance.start());

		persistance.remove(3);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(3, 5), persistance.getNextBlock());
		Assert.assertEquals(3, persistance.start());

		persistance.add(0, 1);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(3, 5), persistance.getNextBlock());
		Assert.assertEquals(3, persistance.start());

		persistance.remove(2);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertNull(persistance.getNextBlock());
		Assert.assertEquals(5, persistance.start());

		persistance.add(0, 10);
		System.out.println(persistance);
		Assert.assertEquals(1, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(5, 10), persistance.getNextBlock());
		Assert.assertEquals(5, persistance.start());

		persistance.remove(2);
		System.out.println(persistance);
		Assert.assertEquals(2, persistance.treeSize());
		Assert.assertEquals(new WrittenInterval(7, 10), persistance.getNextBlock());
		Assert.assertEquals(7, persistance.start());
	}
	
	
	// Not supported, but could throw an exception if overwriting the current start...
	@Test
	public void testDontOverwrite()
	{
	}

	@Test
	public void testDontOverwriteOnWrap()
	{
		
	}

	@Test
	public void fuzzyTest()
	{
		int bufferLength = 64;
		Random random = new Random(1776);
		IntervalPersistance persistance = new IntervalPersistance(bufferLength);
		
		int counter = 0;
		boolean[] written = new boolean[bufferLength];
		for (int j = 0; j < 1000000; j++)
		{
			int blockBegin = counter;
			int blockEnd = blockBegin;
			for (int i=counter;i<written.length;i++)
			{
				if (written[i])
				{
					blockEnd++;
				}
				else
				{
					break;
				}
			}
			
//			System.out.println("counter: " + counter);
//			System.out.println("block: " + blockBegin + "-" + blockEnd);
//			System.out.println("tree: " + persistance);
			Assert.assertEquals(counter, persistance.start());
			if (blockBegin == blockEnd)
			{
				Assert.assertNull(persistance.getNextBlock());
			}
			else
			{
				Assert.assertEquals(new WrittenInterval(blockBegin, blockEnd), persistance.getNextBlock());
			}
			
			if (blockBegin == blockEnd || random.nextDouble() < .5)
			{
				// write
				int begin = random.nextInt(written.length);
				if (random.nextDouble() < .5)
				{
					begin = Math.min(written.length - 1, Math.max(0, (int) (counter + 10 * random.nextGaussian())));
				}
				
				int end = begin + random.nextInt(Math.min(32, written.length - begin + 1));

				for (int i = begin; i < end; i++)
				{
					written[i] = true;
				}
				persistance.add(begin, end);
//				System.out.println("wrote " + begin + " to " + end);
			}
			else
			{
				// read
				int length = random.nextInt(blockEnd - blockBegin + 1);
//				System.out.println("read " + length);

				persistance.remove(length);
				for (int i = counter; i < counter + length; i++)
				{
					written[i] = false;
				}
				counter += length;
				if (counter >= bufferLength)
				{
					counter = 0;
				}
			}
		}
	}
}
