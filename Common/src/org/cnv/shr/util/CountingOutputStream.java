
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


package org.cnv.shr.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.json.stream.JsonGenerator;

public final class CountingOutputStream extends OutputStream
{
	private OutputStream delegate;
	private long soFar;
	
	
	

private OutputStream logFile; 
{ 
  Map<String, Object> properties = new HashMap<>(1);
  properties.put(JsonGenerator.PRETTY_PRINTING, true);
	try
	{
		logFile = Files.newOutputStream(Paths.get("log.out." + System.currentTimeMillis() + "." + Math.random() + ".txt"));
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public CountingOutputStream(OutputStream newInputStream)
	{
		this.delegate = newInputStream;
	}
	
	public void stopOtherSide() throws IOException
	{
		delegate.write(13);
		delegate.write(0);
	}

	public long getSoFar()
	{
		return soFar;
	}

	@Override
	public void write(int b) throws IOException
	{
		soFar++;
		delegate.write(b);
		if (b == 13)
		{
			delegate.write(b);
		}
		logFile.write(b);
	}

	public void flush() throws IOException
	{
		delegate.flush();
		logFile.flush();
	}
	
	@Override
	public void close() throws IOException
	{
		stopOtherSide();
	}
	
	public void actuallyClose() throws IOException
	{
		delegate.close();
		logFile.close();
	}
}
