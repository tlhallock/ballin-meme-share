
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.updt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.TimerTask;
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
				ensureEventuallyClosed(socket);
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
				Updater.code.checkTime();
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

	private void ensureEventuallyClosed(Socket socket)
	{
		Misc.timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				try
				{
					if (!socket.isClosed())
					{
						socket.close();
					}
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Error while closing code:", e);
				}
			}
		}, 10 * 60 * 1000);
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
