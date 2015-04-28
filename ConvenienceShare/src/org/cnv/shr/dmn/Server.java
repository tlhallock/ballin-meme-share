package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.cnv.shr.msg.Message;

public class Server
{
	void runLoop() throws IOException
	{
		ServerSocket socket = new ServerSocket(Settings.getInstance().getDefaultPort());
		for (;;)
		{
			Socket accept = socket.accept();
			try (InputStream inputStream = accept.getInputStream();)
			{
				Message request = Message.readMsg(inputStream);
				if (!request.authenticate())
				{
					continue;
				}

				try
				{
					request.perform();
				}
				catch (Exception e)
				{
					// OutputStream outputStream = accept.getOutputStream();
					e.printStackTrace();
				}
			}
		}
	}
}
