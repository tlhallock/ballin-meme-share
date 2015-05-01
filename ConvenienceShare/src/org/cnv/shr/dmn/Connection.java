package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	
	private boolean allDone = false;

	/** Initiator **/
	public Connection(String ip, int port) throws UnknownHostException, IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		socket = new Socket(ip, port);
		output = new GZIPOutputStream(socket.getOutputStream());
		input =  new GZIPInputStream(socket.getInputStream());
	}
	
	/** Receiver **/
	public Connection(Socket socket) throws IOException
	{
		lastActivity = connectionOpened = System.currentTimeMillis();
		this.socket = socket;
		input =  new GZIPInputStream(socket.getInputStream());
		output = new GZIPOutputStream(socket.getOutputStream());
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
				
				lastActivity = System.currentTimeMillis();

				try
				{
					request.perform(this);
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
			Services.logger.logStream.println("Sending message of type " + m.getClass().getName()
					+ " to " + socket.getInetAddress() + ":" + socket.getPort());
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
		try
		{
			output.close();
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to close output stream.");
			e.printStackTrace(Services.logger.logStream);
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
			Services.logger.logStream.println("Unable to close input stream.");
			e.printStackTrace(Services.logger.logStream);
		}
	}
}
