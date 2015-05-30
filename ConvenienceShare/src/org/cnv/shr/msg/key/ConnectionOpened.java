package org.cnv.shr.msg.key;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

import de.flexiprovider.core.rijndael.RijndaelKey;

public class ConnectionOpened extends KeyMessage
{
	private byte[] decryptedNaunce;
	
	public ConnectionOpened(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public ConnectionOpened(byte[] encoded) throws IOException
	{
		this.decryptedNaunce = encoded;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		decryptedNaunce = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(decryptedNaunce);
	}

	public static int TYPE = 24;
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	public String toString()
	{
		return "You are authenticated.";
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			connection.setAuthenticated(true);
		}
		else
		{
			fail("Connection opened: failed first naunce.", connection);
		}
	}
	
	
	
}
