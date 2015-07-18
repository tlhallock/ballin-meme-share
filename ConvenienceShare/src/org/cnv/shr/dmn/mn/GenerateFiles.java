package org.cnv.shr.dmn.mn;

import java.io.IOException;

import org.cnv.shr.test.TestUtils;

public class GenerateFiles
{
	public static void main(String[] args) throws IOException
	{
		TestUtils.makeSampleDirectories("/home/thallock/Documents/smallfiles", 5, 10, 1024 * 1024, 500);
	}
}
