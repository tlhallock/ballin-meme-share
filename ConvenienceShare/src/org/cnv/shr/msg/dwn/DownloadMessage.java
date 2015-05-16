package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public abstract class DownloadMessage extends Message
{
	private SharedFileId descriptor;
	
	protected DownloadMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	protected DownloadMessage(SharedFileId descriptor)
	{
		this.descriptor = descriptor;
	}
	
	@Override
	protected final void parse(ByteReader reader) throws IOException
	{
		descriptor = reader.readSharedFileId();
		finishParsing(reader);
	}

	@Override
	protected final void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(descriptor);
		finishWriting(buffer);
	}
	
	protected abstract void finishParsing(ByteReader reader) throws IOException;
	protected abstract void finishWriting(AbstractByteWriter buffer) throws IOException;
	
	protected SharedFileId getDescriptor()
	{
		return descriptor;
	}
}
