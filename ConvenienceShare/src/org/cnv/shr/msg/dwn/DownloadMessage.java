package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public abstract class DownloadMessage extends Message
{
	private FileEntry descriptor;
	
	protected DownloadMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	protected DownloadMessage(FileEntry descriptor)
	{
		this.descriptor = descriptor;
	}
	
	@Override
	protected final void parse(ByteReader reader) throws IOException
	{
		String checksum = reader.readString();
		long fileSize = reader.readLong();
		descriptor = new FileEntry(checksum, fileSize);
		finishParsing(reader);
	}

	@Override
	protected final void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(descriptor.getChecksum());
		buffer.append(descriptor.getFileSize());
		finishWriting(buffer);
	}
	
	protected abstract void finishParsing(ByteReader reader) throws IOException;
	protected abstract void finishWriting(AbstractByteWriter buffer) throws IOException;
	
	public LocalFile getLocal()
	{
		return DbFiles.getFile(descriptor.getChecksum(), descriptor.getFileSize());
	}

	protected FileEntry getDescriptor()
	{
		return descriptor;
	}
}
