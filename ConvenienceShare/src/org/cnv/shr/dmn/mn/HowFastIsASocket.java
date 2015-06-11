
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




package org.cnv.shr.dmn.mn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HowFastIsASocket {

	public static void main(String[] args) throws IOException
	{
		Path p = Paths.get("/media/thallock/OS_Install/Users/thallock/Downloads/kubuntu-15.04-desktop-amd64.iso");
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					otherThread();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}).start();

		
		try (SeekableByteChannel newByteChannel = Files.newByteChannel(p);
			 ServerSocketChannel server = ServerSocketChannel.open();)
		{
			server.socket().bind(new InetSocketAddress(9999));
			try (SocketChannel accept = server.accept();)
			{
				// copy
			}
		}
	}

	private static void otherThread() throws UnknownHostException, IOException
	{
			try (SocketChannel socket = SocketChannel.open();)
			{
				socket.bind(new InetSocketAddress("127.0.0.1", 9999));
				
				// read....
			}
	}
}
