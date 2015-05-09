package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.msg.Failure;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.ByteListBuffer;

public class DownloadFailure extends Failure
{
	public DownloadFailure(String message)
	{
		super(message);
	}

	public DownloadFailure(InetAddress address, InputStream stream) throws IOException
	{
		super(address, stream);
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
