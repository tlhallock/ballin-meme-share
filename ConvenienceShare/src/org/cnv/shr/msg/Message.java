package org.cnv.shr.msg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.sql.SQLException;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public abstract class Message
{
	private static final int VERSION = 1;
	
	private String originatorIdentifier;
	private int port;
	private int nports;
	private int size;
	
	// need to have a version
	
	// These are not used yet...
	private byte[] naunce;
	private byte[] encrypted;
	
	String ip;
	
	/** Outgoing Message **/
	protected Message()
	{
		originatorIdentifier = Services.settings.machineIdentifier.get();
		port = Services.settings.servePortBegin.get();
		nports = Services.settings.maxServes.get();   
		naunce = Misc.getNaunce();
	}
	
	/** Message received 
	 * @throws IOException **/
	protected Message(InetAddress address, InputStream stream) throws IOException
	{
		originatorIdentifier = ByteReader.readString(stream);
		port    = ByteReader.readInt(stream);
		nports  = ByteReader.readInt(stream);
		size    = ByteReader.readInt(stream);
		
		ip = Misc.getIp(address.getAddress());
	}
	
	public Machine getMachine()
	{
		Machine m = DbMachines.getMachine(originatorIdentifier);
		if (m == null)
		{
			return null;
		}
		
		// last active?
		if (m.getPort() != port || !m.getIp().equals(ip) || m.getNumberOfPorts() != nports)
		{
			m.setPort(port);
			m.setIp(ip);
			m.setNumberOfPorts(nports);
			try
			{
				m.save();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		
		return m;
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
		
		header.append(originatorIdentifier);
		header.append(port                );
		header.append(nports              );
		header.append(bytes.length);
		header.append(bytes);
		
		return header.getBytes();
	}

	public boolean authenticate()
	{
		return true;
//		return getMachine().isSharing();
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Please implement toString() in class " + getClass().getName());
		
		return builder.toString();
	}

	protected abstract void parse(InputStream bytes) throws IOException;
	protected abstract void write(ByteListBuffer buffer);
	protected abstract int getType();
	public abstract void perform(Communication connection) throws Exception;
	
}
