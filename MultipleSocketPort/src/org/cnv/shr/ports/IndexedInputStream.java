package org.cnv.shr.ports;

import java.io.IOException;
import java.io.InputStream;

public abstract class IndexedInputStream extends InputStream
{
	public abstract void write(byte[] other, int offset, int length, long startOffset) throws IOException;
}

