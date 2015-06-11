
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

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.EmptyMessage;

public class OutputStreamFlusher extends OutputStream
{
	private boolean written;
	private OutputStream inner;
	private Communication outer;
	
	public OutputStreamFlusher(Communication outer, OutputStream inner)
	{
		this.outer = outer;
		this.inner = inner;
	}
	
	public void flushPending() throws IOException
	{
		written = false;
		EmptyMessage flusher = new EmptyMessage(DoneMessage.DONE_PADDING);
		while (!written)
		{
			outer.send(flusher);
		}
	}
	
	@Override
	public void write(int arg0) throws IOException
	{
		inner.write(arg0);
		written = true;
	}
}
