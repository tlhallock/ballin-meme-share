package org.cnv.shr.dmn;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main
{
	public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException
	{
		Services.initialize();
	}

	public static void quit()
	{
		Services.deInitialize();
	}
}
