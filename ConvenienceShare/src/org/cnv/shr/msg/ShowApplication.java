package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class ShowApplication extends Message
{
	public static final int TYPE = 40;
	
	public ShowApplication() {}
	
	public ShowApplication(InputStream input) throws IOException
	{
		super(input);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException {}
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	@Override
	public void perform(Communication connection) throws Exception
	{
		InetSocketAddress remoteAddress = (InetSocketAddress) connection.getSocket().getRemoteSocketAddress();
		String hostAddress = remoteAddress.getAddress().getHostAddress();
		HashSet<String> collectIps = Misc.collectIps();
		
		LogWrapper.getLogger().info("Remote: " + hostAddress);
		LogWrapper.getLogger().info("Locals: " + collectIps);
		
		if (!collectIps.contains(hostAddress))
		{
			return;
		}
		UserActions.showGui();
	}
	
	@Override
	public boolean requiresAthentication()
	{
		return false;
	}
	
}