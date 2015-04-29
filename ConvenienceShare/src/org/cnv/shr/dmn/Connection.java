package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.Message;

public class Connection implements Runnable
{
	private Socket socket;
	private InputStream input;
	private OutputStream output;

	public Connection(Machine m) throws UnknownHostException, IOException
	{
		socket = m.open();
		output = socket.getOutputStream();
		input = socket.getInputStream();
	}
	
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		input = socket.getInputStream();
		output = socket.getOutputStream();
	}
	
	public void run()
	{
		try
		{
			for (;;)
			{
				Message request = Services.msgReader.readMsg(socket.getInetAddress(), input);
				if (request == null || !request.authenticate())
				{
					return;
				}

				try
				{
					request.perform();
				}
				catch (Exception e)
				{
					e.printStackTrace(Services.logger.logStream);
				}
			}
		}
		catch (Exception ex)
		{
			Services.logger.logStream.println(ex);
		}
	}

	public void send(Message m)
	{
		try
		{
			Services.logger.logStream.println("Sending message of type " + m.getClass()
					+ " to " + socket.getInetAddress() + ":" + socket.getPort());
			output.write(m.getBytes());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
