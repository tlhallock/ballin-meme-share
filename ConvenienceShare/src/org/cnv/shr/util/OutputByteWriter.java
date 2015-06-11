
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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class OutputByteWriter extends AbstractByteWriter implements Closeable
{
	private OutputStream output;
	ConnectionStatistics stats;
	long length;

	public OutputByteWriter(OutputStream output)
	{
		this.output = output;
	}
	
	public OutputByteWriter(OutputStream output, ConnectionStatistics stats)
	{
		this.output = output;
		this.stats = stats;
	}

	@Override
	public AbstractByteWriter append(byte[] bytes)  throws IOException
	{
		output.write(bytes);
		length += bytes.length;
		return this;
	}

	@Override
	public AbstractByteWriter append(byte b) throws IOException
	{
		output.write(b);
		length++;
		return this;
	}

	@Override
	public long getLength()
	{
		return length;
	}

	@Override
	public void close() throws IOException
	{
		output.close();
	}
}
