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
