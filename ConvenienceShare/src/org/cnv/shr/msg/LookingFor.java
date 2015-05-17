package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class LookingFor extends Message
{
	public static int TYPE = 28;
	
	private String checksum;
	private long fileSize;
	
	
	public LookingFor(SharedFile file)
	{
		checksum = file.getChecksum();
		fileSize = file.getFileSize();
	}

	public LookingFor(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{

	}

	@Override
	protected void print(AbstractByteWriter buffer)
	{

	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.send(new MachineHasFile(DbFiles.getFile(checksum, fileSize)));
	}
}
