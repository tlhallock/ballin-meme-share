package org.cnv.shr.mdl;

import java.io.InputStream;

import org.cnv.shr.util.ByteListBuffer;

public interface NetworkObject
{
	void read (InputStream input);
	void write(ByteListBuffer buffer);
}
