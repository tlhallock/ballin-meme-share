package org.cnv.shr.dmn;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		try
		{
			if (args.length < 1)
			{
				args = new String[] { "/work/ballin-meme-share/runDir/settings1.props" };
			}
			
			Services.initialize(args);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			quit();
		}
		
		// black list
		// larger auth in first message
		// make standalone key server
		// make database text size correct
		
		// test same number of files
		// test file added
		// test file removed
		// test file downloaded
		// test list dir
		// test byte buffer
		// test format
		
		// add setting for checksum
		// add setting for ip address
		// add setting for sync repeat
		
		// start on startup
	}

	private static boolean quitting = false;
	public static void quit()
	{
		if (quitting)
		{
			return;
		}
		else
		{
			quitting = true;
		}
		try
		{
			Services.deInitialize();
		}
		finally
		{
			System.exit(0);
		}
	}
}
