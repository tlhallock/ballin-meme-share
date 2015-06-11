
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


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.CountingOutputStream;


public class Test
{

	public static void main(String[] args) throws IOException
	{
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
			}}).start();
		
		try (ServerSocket server = new ServerSocket(9999);
				Socket accept = server.accept();
				JsonGenerator generator = TrackObjectUtils.createGenerator(new CountingOutputStream(accept.getOutputStream()));
				JsonParser    parser    = TrackObjectUtils.createParser   (new CountingInputStream(accept.getInputStream()));)
		{
			generator.writeStartArray();
			generator.write("foobar");
			generator.flush();
			
			String string = null;
			parser.next(); // start
			parser.next(); // value
			string = parser.getString();
			
			generator.writeEnd();
			generator.flush();
			
			parser.next(); // end

			System.out.println("Found string " + string);
		}
	}

	private static void otherThread() throws UnknownHostException, IOException
	{
			try (Socket accept = new Socket("127.0.0.1", 9999);
					JsonGenerator generator = TrackObjectUtils.createGenerator(new CountingOutputStream(accept.getOutputStream()));
					JsonParser    parser    = TrackObjectUtils.createParser   (new CountingInputStream(accept.getInputStream()));)
			{
				generator.writeStartArray();
				generator.write("foobar");
				generator.flush();
				
				String string = null;
				parser.next(); // start
				parser.next(); // value
				string = parser.getString();
				
				generator.writeEnd();
				generator.flush();
				
				parser.next(); // end

				System.out.println("Found string " + string);
			}
	}
}
