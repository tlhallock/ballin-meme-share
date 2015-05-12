package org.cnv.shr.test;

import org.cnv.shr.dmn.Main;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class BaseTest
{
	// create clean instance(ip)
	
	// checks:
	// no database errors
	// knows about machine
	// files match
	// knows about file
	// does not know about file
	// has connection
	// list keys
	// close other instance... 
	
	
	@Before
	public void setUp() throws Exception
	{
		Main.main(new String[] {"-f", "/work/ballin-meme-share/runDir/settings1.props"});
	}

	@After
	public void tearDown() throws Exception
	{
		Main.quit();
	}

	@Test
	public void test()
	{
	}
}
