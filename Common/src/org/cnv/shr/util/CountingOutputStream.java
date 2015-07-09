
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

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
	public void write(int b) throws IOException
	{
		soFar++;
		stats.wrote(b);
		delegate.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		soFar += len;
		stats.wrote(b, off, len);
		delegate.write(b, off, len);
	}
	
	private static final ByteStats stats = new ByteStats();
	private static final class ByteStats extends TimerTask
	{
		double[] histogram = new double[256];
		{
			new Timer().scheduleAtFixedRate(this, 1000, 1000);
		}

		@Override
		public void run()
		{
			try (BufferedWriter output = Files.newBufferedWriter(Paths.get("stats.txt")))
			{
				for (int i = 0; i < histogram.length; i++)
				{
					output.write(String.format("%d\t:\t%12.4f\n", i, histogram[i]));
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		void wrote(byte[] bytes, int off, int len)
		{
			for (int i=0;i<len;i++)
			{
				wrote(bytes[i + off]);
			}
		}
		
		void wrote(int b)
		{
			histogram[b & 0xff]++;
		}
	}
}
