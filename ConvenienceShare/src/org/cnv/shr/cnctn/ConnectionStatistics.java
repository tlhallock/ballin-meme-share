package org.cnv.shr.cnctn;

public class ConnectionStatistics
{
	private long lastKbpsRefresh = System.currentTimeMillis();
	private boolean activity;
	
	private long numBytesSent;
	private long numBytesReceived;
	private long connectionAuthenticated;
	
	public void bytesRead(int numBytes)
	{
		numBytesReceived += numBytes;
	}
	
	public void bytesSent(int numBytes)
	{
		numBytesSent += numBytes;
	}
	
	public String getBitsUp(long now)
	{
		return String.valueOf(1000.0 * numBytesSent / (double) (now - lastKbpsRefresh));
	}
	public String getBitsDown(long now)
	{
		return String.valueOf(1000.0 * numBytesReceived / (double) (now - lastKbpsRefresh));
	}
	public boolean isAuthenticated()
	{
		return false;
	}
	public boolean isActive()
	{
		return activity;
	}
	
	public void setLastRefresh(long now)
	{
		lastKbpsRefresh = now;
		activity = false;
	}
	
	
}
