package org.cnv.shr.test;

import org.cnv.shr.util.Find;
import org.cnv.shr.util.Misc;

public class TestFind {

	public static void main(String[] args)
	{
		System.out.println(Misc.formatNumberOfFiles(100_000_000_000_000L));
		
		
		
		Find find = new Find("/home/rever/Documents");
		while (find.hasNext())
		{
			System.out.println(find.next());
		}
	}
}
