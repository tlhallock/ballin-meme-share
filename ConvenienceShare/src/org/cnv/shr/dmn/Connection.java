package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.Message;

public class Connection implements Runnable
{
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	
	private boolean allDone = false;

	/** Initiator **/
	public Connection(Machine m) throws UnknownHostException, IOException
	{
		socket = m.open();
		output = socket.getOutputStream();
		input = socket.getInputStream();
	}
	
	/** Receiver **/
	public Connection(Socket socket) throws IOException
	{
		this.socket = socket;
		input = socket.getInputStream();
		output = socket.getOutputStream();
	}
	
	public String getUrl()
	{
		return socket.getInetAddress() + ":" + socket.getPort();
	}
	
	public void run()
	{
		try
		{
			while (!allDone)
			{
				Message request = Services.msgReader.readMsg(socket.getInetAddress(), input);
				if (request == null || !request.authenticate())
				{
					return;
				}

				try
				{
					request.perform(null);
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
		notifyDone();
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
	
	public void notifyDone()
	{
		send(new DoneMessage());
		try
		{
			output.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void remoteIsDone()
	{
		allDone = true;
		try
		{
			input.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
