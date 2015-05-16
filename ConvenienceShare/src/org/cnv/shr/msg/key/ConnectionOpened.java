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
	private byte[] encryptedAesKey;
	
	public ConnectionOpened(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	public ConnectionOpened(RijndaelKey aesKey, byte[] encoded, PublicKey pKey) throws IOException
	{
		this.decryptedNaunce = encoded;
		Services.keyManager.encrypt(pKey, aesKey.getEncoded());
		this.encryptedAesKey = getBytes(pKey, aesKey);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		decryptedNaunce = reader.readVarByteArray();
		encryptedAesKey = reader.readVarByteArray();
	}

	@Override
	protected void write(AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(decryptedNaunce);
		buffer.appendVarByteArray(encryptedAesKey);
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
			connection.setAuthenticated(getKey(connection.getAuthentication().getLocalKey(), encryptedAesKey));
		}
		else
		{
			fail("Connection opened: failed first naunce.", connection);
		}
	}
	
	private static RijndaelKey getKey(PublicKey pKey, byte[] bytes) throws IOException, ClassNotFoundException
	{
		try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(
				Services.keyManager.decrypt(pKey, bytes)));)
		{
			return (RijndaelKey) objectInputStream.readObject();
		}
	}
	
	private static byte[] getBytes(PublicKey key, RijndaelKey aesKey) throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);)
		{
			objectOutputStream.writeObject(aesKey);
		}
		return Services.keyManager.encrypt(key, byteArrayOutputStream.toByteArray());
	}
}
