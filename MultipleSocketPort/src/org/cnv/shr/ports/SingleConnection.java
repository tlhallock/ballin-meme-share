package org.cnv.shr.ports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SingleConnection
{
	WindowInputStream inputStream;
	WindowOutputStream outputStream;
	
	
	public InputStream getInputStream() throws IOException
	{
		return inputStream;
	}
	public OutputStream getOutputStream() throws IOException
	{
		return outputStream;
	}
	
	public void shutdownOutput() throws IOException
	{
		outputStream.close();
	}
	
	public void shutdownInput() throws IOException
	{
		inputStream.close();
	}
	
	public synchronized void close() throws IOException
	{
		shutdownInput();
		shutdownOutput();
	}
	
	public boolean isClosed() throws IOException
	{
		return inputStream.isClosed() && outputStream.isClosed();
	}
	
	public void sendAmountRead()
	{
		long amountRead = inputStream.getAmountRead();
	}
}
