package org.cnv.shr.test;

import java.io.File;
import java.io.PrintStream;

import org.cnv.shr.util.CircularOutputStream;

public class TestCircular
{

	public static void main(String[] args) throws Exception
	{
		File file = new File("something.txt");
		try (PrintStream ps = new PrintStream(new CircularOutputStream(file, 100));)
		{
//			for (int i = 0; i < 15; i++)
			{
				ps.println("This is string 1");
				ps.println("This is string 2");
				ps.println("This is string 3");
			}
		}
		file.delete();
	}
}
