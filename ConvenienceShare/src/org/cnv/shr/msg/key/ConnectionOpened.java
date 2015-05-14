package org.cnv.shr.msg.key;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionStatistics;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

import de.flexiprovider.core.rijndael.RijndaelKey;

public class ConnectionOpened extends KeyMessage
{
	private RijndaelKey aesKey;
	byte[] decryptedNaunce;

	public ConnectionOpened(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public ConnectionOpened(RijndaelKey aesKey, byte[] encoded)
	{
		this.decryptedNaunce = encoded;
		this.aesKey = aesKey;
	}

	@Override
	protected void parse(InputStream bytes, ConnectionStatistics stats) throws IOException
	{
		decryptedNaunce = ByteReader.readVarByteArray(bytes);
		try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(ByteReader.readVarByteArray(bytes)));)
		{
			try
			{
				aesKey = (RijndaelKey) objectInputStream.readObject();
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(decryptedNaunce);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);)
		{
			objectOutputStream.writeObject(aesKey);
		}
		buffer.appendVarByteArray(byteArrayOutputStream.toByteArray());
	}

	public static int TYPE = 24;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		if (connection.getAuthentication().hasPendingNaunce(decryptedNaunce))
		{
			connection.setAuthenticated(aesKey);
		}
		else
		{
			fail(connection);
		}
	}
}
