package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class UpdateCode extends Message
{
	public static int TYPE = 9;
	
	private String url;
	byte[] decryptedCodeNaunce;

	public UpdateCode(InputStream i) throws IOException
	{
		super(i);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void print(AbstractByteWriter buffer)
	{
		// TODO Auto-generated method stub
		
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (!Arrays.equals(
				connection.getAuthentication().getRemoteKey().getEncoded(),
				Services.keyManager.getCodeUpdateKey().getEncoded()))
		{
			LogWrapper.getLogger().info("Not able to update code: key failure.");
			return;
		}
		// download
		// restart
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Can I update your code.");
		
		return builder.toString();
	}
}
