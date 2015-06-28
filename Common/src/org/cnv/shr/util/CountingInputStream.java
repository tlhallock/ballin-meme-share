
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

public final class CountingInputStream extends InputStream
{
	private InputStream delegate;
	private long soFar;
	
	public CountingInputStream(InputStream newInputStream)
	{
		this.delegate = newInputStream;
	}
	
	@Override
	public int read() throws IOException
	{
		int read = delegate.read();
		if (read > 0)
		{
			soFar++;
		}
		return read;
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
		delegate.close();
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
		int read = delegate.read(b, off, len);
		if (read > 0)
		{
			soFar += read;
		}
		return read;
	}
}
