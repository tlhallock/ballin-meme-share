package org.cnv.shr.ports;

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
		Assert.assertEquals(0, persistance.start());
	}
	
	
	
	public void testCoalesce()
	{
		
	}
	
	public void testMultipleOnSame()
	{
		
	}

	public void testCoalesceMutliple()
	{
		
	}

	public void testWriteAll()
	{
		
	}
	
	public void testDontOverwrite()
	{
		
	}
	
	public void testDontOverwriteOnWrap()
	{
		
	}
	
	public void fuzzyTest()
	{
		for (;;)
		{
			
		}
	}
}
