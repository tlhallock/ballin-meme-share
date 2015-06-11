
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


package org.cnv.shr.dmn;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;

public class RequestHandler extends Thread
{	
	private boolean quit;
	ServerSocket socket;
	int port;
	
	public RequestHandler(ServerSocket socket)
	{
		this.port = socket.getLocalPort();
		LogWrapper.getLogger().info("Starting on port " + port);
		this.socket = socket;
	}
	
	public void quit()
	{
		quit = true;
		try
		{
			socket.close();
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to close socket.", ex);
		}
	}
	
	@Override
	public void run()
	{
		while (!quit)
		{
			try
			{
				if (socket.isClosed())
				{
					socket = new ServerSocket(port);
				}
				socket.setReuseAddress(true);
				// socket.setSoTimeout(5000);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect on " + port + ": " + e.getMessage(), e);
				continue;
			}

			try (Socket accept = socket.accept();)
			{
				Services.networkManager.handleConnection(accept);
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect on " + port + ": " + e.getMessage(), e);
				continue;
			}
			catch (Exception t)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to handle connection on " + port, t);
			}
		}
		LogWrapper.getLogger().info("Quitting on port " + port);
	}
}
