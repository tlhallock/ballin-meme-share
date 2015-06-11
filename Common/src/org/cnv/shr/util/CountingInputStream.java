
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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.json.stream.JsonGenerator;

public final class CountingInputStream extends InputStream
{
	private InputStream delegate;
	private long soFar;
	private boolean paused;
	
	boolean rawMode;
	
	
	
	
	
	
	
	
	
	

private OutputStream logFile; 
{ 
  Map<String, Object> properties = new HashMap<>(1);
  properties.put(JsonGenerator.PRETTY_PRINTING, true);
	try
	{
		logFile = Files.newOutputStream(Paths.get("log.in" + System.currentTimeMillis() + "." + Math.random() + ".txt"));
	}
	catch (IOException e)
	{
		e.printStackTrace();
	}
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public CountingInputStream(InputStream newInputStream)
	{
		this.delegate = newInputStream;
	}

	public void setRawMode(boolean rawMode)
	{
		this.rawMode = rawMode;
	}
	
	@Override
	public int read() throws IOException
	{
		if (paused)
		{
			return -1;
		}
		soFar++;
		int read = delegate.read();
		logFile.write(read);
		if (!rawMode && read == 13 && delegate.read() != 13)
		{
			paused = true;
			return -1;
		}
		return read;
	}
	
	public void startAgain()
	{
		paused = false;
	}
	
	public long getSoFar()
	{
		return soFar;
	}
	
	public long skip(long n) throws IOException
	{
		return delegate.skip(n);
	}

	public int available() throws IOException
	{
		return delegate.available();
	}

	public void close() throws IOException
	{
//		delegate.close();
	}

	public synchronized void mark(int readlimit)
	{
		delegate.mark(readlimit);
	}

	public synchronized void reset() throws IOException
	{
		delegate.reset();
	}

	public boolean markSupported()
	{
		return delegate.markSupported();
	}
	
	public int read(byte b[], int off, int len) throws IOException
	{
		if (rawMode)
		{
			return delegate.read(b, off, len);
		}
		if (b == null || b.length < 1)
		{
			throw new NullPointerException();
		}
		int read =  read();
		if (read < 0)
		{
			return -1;
		}
		b[off] = (byte) read;
		return 1;
	}
}
