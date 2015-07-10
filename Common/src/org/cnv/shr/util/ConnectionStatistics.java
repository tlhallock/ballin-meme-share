
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

import java.util.Date;



public class ConnectionStatistics
{
	private long lastKbpsRefresh = System.currentTimeMillis();
	private long lastInputOffset;
	private long lastOutputOffset;
	
	private double speedUp;
	private double speedDown;
	
	private long numBytesSent;
	private long numBytesReceived;
	private boolean connectionAuthenticated;
	
	private CountingInputStream input;
	private CountingOutputStream output;
	
	private String lastSentMessageType;
	private String lastReceivedMessageType;
	
	private String reason;

	private long   lastActivity = System.currentTimeMillis();
  private String lastActive = new Date(lastActivity).toString();
  
	
	public ConnectionStatistics(CountingInputStream input, CountingOutputStream output)
	{
		this.input = input;
		this.output = output;
	}
	
	public synchronized void refresh()
	{
		long now = System.currentTimeMillis();
		if (now - lastKbpsRefresh < 1000)
		{
			return;
		}
		long inSoFar = input.getSoFar();
		long outSoFar = output.getSoFar();
		
		numBytesReceived = inSoFar - lastInputOffset;
		numBytesSent = outSoFar - lastOutputOffset;
		lastInputOffset = inSoFar;
		lastOutputOffset = outSoFar;

		speedUp   = 1000.0 * numBytesSent / (now - lastKbpsRefresh);
		speedDown = 1000.0 * numBytesReceived / (now - lastKbpsRefresh);
		
		lastKbpsRefresh = now;

		if (numBytesReceived > 0 || numBytesSent > 0)
		{
			lastActive = "now";
			lastActivity = now;
		}
		else if (now - lastActivity < 5 * 1000)
		{
			lastActive = "now";
		}
		else if (now - lastActivity < 60 * 1000)
		{
			lastActive = "a minute ago";
		}
		else if (now - lastActivity < 10 * 60 * 1000)
		{
			lastActive = "minutes ago";
		}
		else
		{
			lastActive = new Date(lastActivity).toString();
		}
	}

	public double getSpeedUp()
	{
		return speedUp;
	}
	public double getSpeedDown()
	{
		return speedDown;
	}
	
	public String getBitsUp()
	{
		return Misc.formatDiskUsage(speedUp) + "ps";
	}
	public String getBitsDown()
	{
		return Misc.formatDiskUsage(speedDown) + "ps";
	}
	public void setAuthenticated(boolean authenticated)
	{
		this.connectionAuthenticated = authenticated;
	}
	public boolean isAuthenticated()
	{
		return connectionAuthenticated;
	}
	public String getLastActive()
	{
		return lastActive;
	}
	public String getTotalDown()
	{
		return Misc.formatDiskUsage(input.getSoFar());
	}
	public String getTotalUp()
	{
		return Misc.formatDiskUsage(output.getSoFar());
	}
	public String getStatus()
	{
		return "Sent: \"" + lastSentMessageType + "\", Received: \"" + lastReceivedMessageType + "\""; 
	}
	public void setLastSent(String type)
	{
		lastSentMessageType = type;
	}
	public void setLastReceived(String type)
	{
		lastReceivedMessageType = type;
	}
	public void setReason(String reason)
	{
		this.reason = reason;
	}
	public String getReason()
	{
		return reason;
	}
}
