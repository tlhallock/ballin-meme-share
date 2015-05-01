package org.cnv.shr.test;

import org.cnv.shr.util.Find;

public class TestFind {

	public static void main(String[] args)
	{
		Find find = new Find("/home/rever/Documents");
		while (find.hasNext())
		{
			System.out.println(find.next());
		}
	}
}
