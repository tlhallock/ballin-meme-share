
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

public final class CountingOutputStream extends OutputStream
{
	private OutputStream delegate;
	private long soFar;

	public CountingOutputStream(OutputStream newInputStream)
	{
		this.delegate = newInputStream;
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
	}

	public void flush() throws IOException
	{
		delegate.flush();
	}
	
	@Override
	public void close() throws IOException
	{
		delegate.close();
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		soFar += len;
		delegate.write(b, off, len);
	}
}
