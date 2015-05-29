package org.cnv.shr.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cnv.shr.db.h2.PathBreaker;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.CircularOutputStream;
import org.cnv.shr.util.Find;
import org.cnv.shr.util.IpTester;
import org.cnv.shr.util.Misc;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class UtilTests
{
	static File settingsFile = new File("test_settings.props");
	
	@BeforeClass
	public static void setUpClass()
	{
		Services.settings = new Settings(settingsFile);
		Services.settings.logToFile.set(false);
	}

	@AfterClass
	public static void tearDown() throws Exception
	{
		settingsFile.delete();
	}

	@Test
	public void testPathBreaker()
	{
//		String str = "Combinatorial optimization.. theory and algorithms.pdf";
		String str = "/home/thallock/Documents/Combinatorial optimization.. theory and algorithms.pdf";
		System.out.println(str);
		System.out.println(PathBreaker.join(PathBreaker.breakPath(str)));
		System.out.println(PathBreaker.breakPath(str)[PathBreaker.breakPath(str).length-1].getUnbrokenName());
		
		// TODO: asserts
	}
	
	@Test
	public void testIpTester()
	{
		IpTester ipTester = new IpTester();
		System.out.println(ipTester.getIpFromCanYouSeeMeDotOrg());
//		System.out.println(ipTester.testIp(ipTester.getIpFromCanYouSeeMeDotOrg(), 80));
	}
	
	public void testFind()
	{
		System.out.println(Misc.formatNumberOfFiles(100_000_000_000_000L));
		
		Find find = new Find("/home/thallock/Documents");
		int count = 0;
		while (find.hasNext())
		{
			System.out.println(find.next());
			count++;
		}
		System.out.println("Count = " + count);
	}
	
	@Test
	public void testMiscFormat()
	{
		byte[] naunce = Misc.getBytes(27);
		ByteListBuffer buffer = new ByteListBuffer();
		buffer.append(naunce);
		
		Assert.assertArrayEquals(naunce, Misc.format(Misc.format(naunce)));
	}
	
	@Test
	public void testByteListBuffer()
	{
		
	}

	public void testCircular() throws IOException
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
		
		// TODO: asserts...
		file.delete();
	}
	
	@Test
	public void testFs() throws IOException
	{
		Path createTempDirectory = Files.createTempDirectory("root");
		String path = createTempDirectory.toFile().getAbsolutePath();
		TestUtils.makeSampleDirectories(path, 3, 10, 1024, 20);
		Misc.rm(createTempDirectory);
	}
}
