package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.ByteReader;

public abstract class Message
{
	protected String user;
	protected String machine;

	public String getUser()
	{
		return user;
	}

	public Machine getMachine()
	{
		return new Machine(machine);
	}

	public void read(InputStream stream) throws IOException
	{
		user = ByteReader.readString(stream);
		machine = ByteReader.readString(stream);
	}

	void write(ByteListBuffer buffer) throws UnsupportedEncodingException
	{
		buffer.append(user);
		buffer.append(machine);
	}

	final void send(Machine machine) throws UnknownHostException, IOException
	{
		try (Socket socket = machine.open(); OutputStream out = socket.getOutputStream();)
		{
			ByteListBuffer buffer = new ByteListBuffer();
			write(buffer);
			out.write(buffer.getBytes());
		}
		catch (UnsupportedEncodingException ex)
		{
			Services.logger.logStream.println("No UTF-8 support, quiting.");
			ex.printStackTrace(Services.logger.logStream);
			Main.quit();
		}
	}

	public static Message readMsg(InputStream inputStream) throws IOException
	{
		int msgType = inputStream.read();
		Message request = null;

		switch (msgType)
		{
		case 1:
			break;

		default:
			System.out.println("Unknown message type: " + msgType);
			System.out.println("Skipping");
			return null;
		}

		request.read(inputStream);

		return request;
	}

	public boolean authenticate()
	{
		return true;
	}

	public abstract void perform() throws Exception;
}
