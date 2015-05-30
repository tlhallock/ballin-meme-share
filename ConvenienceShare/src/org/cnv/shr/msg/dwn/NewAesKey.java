package org.cnv.shr.msg.dwn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

import de.flexiprovider.core.rijndael.RijndaelKey;

public class NewAesKey extends Message
{
	public static final int TYPE = 34;
	
	private byte[] encryptedAesKey;
	
	public NewAesKey(InputStream input) throws IOException
	{
		super(input);
	}
	
	public NewAesKey(RijndaelKey aesKey, PublicKey pKey) throws IOException
	{
		Services.keyManager.encrypt(pKey, aesKey.getEncoded());
		this.encryptedAesKey = getBytes(pKey, aesKey);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		encryptedAesKey = reader.readVarByteArray();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.appendVarByteArray(encryptedAesKey);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.decrypt(getKey(connection.getAuthentication().getLocalKey(), encryptedAesKey));
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
