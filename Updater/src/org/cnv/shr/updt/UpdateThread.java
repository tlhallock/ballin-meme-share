package org.cnv.shr.updt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;


class UpdateThread extends Thread
{
	@Override
	public void run()
	{
		LogWrapper.getLogger().info("Listening on " + Updater.updateSocket.getInetAddress().getHostAddress() + ":" + Updater.updateSocket.getLocalPort());
		while (true)
		{
			try (Socket socket = Updater.updateSocket.accept();
					InputStream input = socket.getInputStream();
					OutputStream output = socket.getOutputStream();)
			{
				LogWrapper.getLogger().info("Connected to " + socket.getInetAddress().getHostAddress());
				
				// Requires authentication
				if (input.read() != 0)
				{
					LogWrapper.getLogger().info("Authenticating");
					byte[] encrypted = Misc.readBytes(input);
					byte[] decrypted = Updater.service.decrypt(Updater.service.getPrivateKey(), encrypted);
					Misc.writeBytes(decrypted, output);
				}
				
				LogWrapper.getLogger().info("Sending latest version.");
				Misc.writeBytes(Updater.code.getVersion().getBytes("UTF8"), output);
				
				if (input.read() == 0)
				{
					LogWrapper.getLogger().info("Already up to date.");
					continue;
				}
				
				LogWrapper.getLogger().info("Serving.");
				copy(output);
				socket.shutdownOutput();
				LogWrapper.getLogger().info("Done.");
				input.read();
			}
			catch (Exception ex)
			{
				LogWrapper.getLogger().log(Level.INFO, "Error while serving code:", ex);
			}
		}
	}
	
	private void copy(OutputStream out) throws FileNotFoundException, IOException
	{
		byte[] buffer = new byte[1024];
		try (InputStream input = Updater.code.getStream())
		{
			int nread;
			while ((nread = input.read(buffer)) >= 0)
			{
				out.write(buffer, 0, nread);
			}
		}
	}
}
