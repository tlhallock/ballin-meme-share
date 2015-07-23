package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HardToCloseStreams
{
	public static abstract class HardToCloseInputStream extends InputStream
	{
		public abstract void actuallyClose() throws IOException;
	}
	
	public static abstract class HardToCloseOutputStream extends OutputStream
	{
		public abstract void actuallyClose() throws IOException;
	}
	
}
