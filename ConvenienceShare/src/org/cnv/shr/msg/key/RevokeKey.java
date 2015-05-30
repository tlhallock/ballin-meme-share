package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class RevokeKey extends Message
{
	private PublicKey revoke;

	public RevokeKey(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer)
	{
		// TODO Auto-generated method stub
		
	}

	public static final int TYPE = 23;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		DbKeys.removeKey(connection.getMachine(), revoke);
	}

}
