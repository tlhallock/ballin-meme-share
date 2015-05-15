package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;

public class MachineHasFile extends Message
{
	private boolean hasFile;
	private String path;
	private String rootName;
	
	public static int TYPE = 17;
	
	public MachineHasFile(SharedFile file)
	{
		if (file == null)
		{
			hasFile = false;
		}
		else
		{
			hasFile = true;
			path = file.getPath().getFullPath();
			rootName = file.getRootDirectory().getName();
		}
	}

	public MachineHasFile(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
		
	}
	
	@Override
	protected void write(AbstractByteWriter buffer)
	{
		
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		// Maybe I should not have sent it?
		// Maybe this should be to remove the connection already present.
		// No.
		if (!hasFile) return;

		Services.downloads.getDownloadInstance(null).addSeeder(connection.getMachine(), connection);
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I got it! ").append(hasFile).append(":").append(path);
		
		return builder.toString();
	}
}
