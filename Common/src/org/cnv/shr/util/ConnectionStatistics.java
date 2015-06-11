
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


public class ConnectionStatistics
{
	private long lastKbpsRefresh = System.currentTimeMillis();
	private long lastInputOffset;
	private long lastOutputOffset;
	
	private long numBytesSent;
	private long numBytesReceived;
	private boolean connectionAuthenticated;
	
	private CountingInputStream input;
	private CountingOutputStream output;
	
	public ConnectionStatistics(CountingInputStream input, CountingOutputStream output)
	{
		this.input = input;
		this.output = output;
	}
	
	public void refresh()
	{
		lastKbpsRefresh = System.currentTimeMillis();
		long inSoFar = input.getSoFar();
		long outSoFar = output.getSoFar();
		
		numBytesReceived += inSoFar - lastInputOffset;
		numBytesSent += outSoFar - lastOutputOffset;
		lastInputOffset = inSoFar;
		lastOutputOffset = outSoFar;
	}
	
	public String getBitsUp(long now)
	{
		return String.valueOf(1000.0 * numBytesSent / (now - lastKbpsRefresh));
	}
	public String getBitsDown(long now)
	{
		return String.valueOf(1000.0 * numBytesReceived / (now - lastKbpsRefresh));
	}
	public void setAuthenticated(boolean authenticated)
	{
		this.connectionAuthenticated = authenticated;
	}
	public boolean isAuthenticated()
	{
		return connectionAuthenticated;
	}
	public boolean isActive()
	{
		return numBytesReceived >= 0 || numBytesSent >= 0;
	}
}
