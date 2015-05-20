package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class MachineHasFile extends DownloadMessage
{
	private boolean hasFile;
	
	public static int TYPE = 17;
	
	public MachineHasFile(SharedFile file)
	{
		super(new SharedFileId(file));
		
		if (file == null)
		{
			hasFile = false;
		}
		else
		{
			hasFile = true;
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
	protected void finishParsing(ByteReader reader) throws IOException
	{
		hasFile = reader.readBoolean();
	}
	
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(hasFile);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		// Maybe I should not have sent it?
		// Maybe this should be to remove the connection already present.
		// No.
		if (!hasFile) return;
		

		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor(), connection);
		if (downloadInstance == null)
		{
			return;
		}
		downloadInstance.addSeeder(connection.getMachine(), connection);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I got it! ").append(hasFile).append(":").append(getDescriptor());
		
		return builder.toString();
	}
}
