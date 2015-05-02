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
	private long connectionOpened;
	private long lastActivity;
	
	private Socket socket;
	private InputStream input;
	private OutputStream output;
	
	private Machine machine;
	
	private boolean done = false;

	/** Initiator **/
	public Connection(String ip, int port) throws UnknownHostException, IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		socket = new Socket(ip, port);
		output = socket.getOutputStream();
		input =  socket.getInputStream();
	}
	
	/** Receiver **/
	public Connection(Socket socket) throws IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		this.socket = socket;
		input =  socket.getInputStream();
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
			while (!done)
			{
				Message request = Services.msgReader.readMsg(socket.getInetAddress(), input);
				if (request == null || !request.authenticate())
				{
					return;
				}
				
				lastActivity = System.currentTimeMillis();

				try
				{
					request.perform(this);
				}
				catch (Exception e)
				{
					Services.logger.logStream.println("Error performing message task:");
					e.printStackTrace(Services.logger.logStream);
				}
			}
		}
		catch (Exception ex)
		{
			Services.logger.logStream.println("Error with connection:");
			ex.printStackTrace(Services.logger.logStream);
		}
		notifyDone();
		try
		{
			socket.close();
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to close socket.");
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public void send(Message m)
	{
		Services.logger.logStream.println("Sending message of type " + m.getClass().getName()
				+ " to " + socket.getInetAddress() + ":" + socket.getPort());
		try
		{
			output.write(m.getBytes());
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to send message: " + m.getClass().getName());
			e.printStackTrace(Services.logger.logStream);
		}
	}
	
	public void notifyDone()
	{
		send(new DoneMessage());
//		try
//		{
//			socket.shutdownOutput();
//		}
//		catch (IOException e)
//		{
//			Services.logger.logStream.println("Unable to close output stream.");
//			e.printStackTrace(Services.logger.logStream);
//		}
	}

	public void remoteIsDone()
	{
		done = true;
//		try
//		{
//			socket.shutdownInput();
//		}
//		catch (IOException e)
//		{
//			Services.logger.logStream.println("Unable to close input stream.");
//			e.printStackTrace(Services.logger.logStream);
//		}
	}
}
