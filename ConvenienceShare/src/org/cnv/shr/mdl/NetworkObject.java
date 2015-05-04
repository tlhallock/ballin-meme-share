package org.cnv.shr.mdl;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.util.ByteListBuffer;

public interface NetworkObject
{
	void read (InputStream input) throws IOException;
	void write(ByteListBuffer buffer);
}
