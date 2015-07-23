package org.cnv.shr.phone.cmn;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.msg.ClientInfo;
import org.cnv.shr.phone.msg.PhoneMessage;


public class PhoneLine implements Closeable
{
	public Socket socket;

	public ConnectionParams params;
	
	public InputStream input;
	public OutputStream output;
	
	private JsonGenerator generator;
	private JsonParser parser;
	
	private ClientInfo info;
	
	public PhoneLine(Socket socket, boolean direct) throws IOException
	{
		this.socket = socket;
		this.input = socket.getInputStream();
		this.output = socket.getOutputStream();
		this.generator = null;
		this.parser = null;
		if (!direct)
		{
			generator = Services.createGenerator(output, false);
			generator.writeStartObject();
			generator.flush();
			
			parser = Services.createParser(input);
			parser.next(); // skip the first write start object...
		}
		params = new ConnectionParams();
	}
	
	public PhoneMessage readMessage() throws IOException
	{
		synchronized (parser)
		{
			return Services.handler.parse(parser, params);
		}
	}
	
	public void sendMessage(PhoneMessage message)
	{
		synchronized (generator)
		{
			message.generate(generator, message.getJsonKey());
		}
	}
	
	@Override
	public void close() throws IOException
	{
		try
		{
			if (generator != null)
				try
				{
					generator.writeEnd();
				}
				finally
				{
					generator.close();
				}
			if (parser != null)
				parser.close();
		}
		finally
		{
			socket.close();
		}
	}

	public ClientInfo getInfo()
	{
		return info;
	}

	public void setInfo(ClientInfo clientInfo)
	{
		this.info = clientInfo;
		clientInfo.setIp(socket.getInetAddress().getHostAddress());
	}

	public void flush() throws IOException
	{
		if (generator != null)
		{
			generator.flush();
		}
		else
		{
			output.flush();
		}
	}
}
