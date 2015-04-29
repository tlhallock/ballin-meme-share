package org.cnv.shr.msg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public abstract class Message
{
	private String ip;
	private int port = Services.settings.defaultPort;
	private int size;
	
	// These are not used yet...
	private byte[] naunce;
	private byte[] encrypted;
	
	/** Outgoing Message **/
	protected Message()
	{
		ip = Services.settings.getLocalIp();
		port = Services.settings.defaultPort;
		naunce = Misc.getNaunce();
	}
	
	/** Message received 
	 * @throws IOException **/
	protected Message(InetAddress address, InputStream stream) throws IOException
	{
		port = (int) ByteReader.readInt(stream);
		size = (int) ByteReader.readInt(stream);
	}
	
	public Machine getMachine()
	{
		return new Machine(ip, port, Services.remotes.getKeys(ip, port));
	}

	public void read(InputStream stream) throws IOException
	{
		byte[] msgData = new byte[size];
		int offset = 0;
		int bytesRead;
		while (offset < size && (bytesRead = stream.read(msgData, offset, size - offset)) >= 0)
		{
			offset += bytesRead;
		}
		if (offset < size)
		{
			throw new IOException("Message is missing bytes! expected " + size + "bytes, but found " + offset);
		}
		
		// decrypt
		
		parse(new ByteArrayInputStream(msgData));
	}
	
	public final byte[] getBytes()
	{
		ByteListBuffer buffer = new ByteListBuffer();
		write(buffer);
		byte[] bytes = buffer.getBytes();

		// encrypt

		ByteListBuffer header = new ByteListBuffer();
		header.append(getType());
		header.append(port);
		header.append(bytes.length);
		header.append(bytes);
		
		return header.getBytes();
	}

	public boolean authenticate()
	{
		return true;
	}

	protected abstract void parse(InputStream bytes) throws IOException;
	protected abstract void write(ByteListBuffer buffer);
	protected abstract int getType();
	public abstract void perform() throws Exception;
	
}
