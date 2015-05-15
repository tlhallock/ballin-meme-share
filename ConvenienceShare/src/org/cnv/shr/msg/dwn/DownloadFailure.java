package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.msg.Failure;

public class DownloadFailure extends Failure
{
	public DownloadFailure(String message)
	{
		super(message);
	}

	public DownloadFailure(InputStream stream) throws IOException
	{
		super(stream);
	}

	public static int TYPE = 21;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection)
	{
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(connection);
		downloadInstance.removePeer(connection);
	}

}