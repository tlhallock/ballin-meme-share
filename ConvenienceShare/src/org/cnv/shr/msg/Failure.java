package org.cnv.shr.msg;

public class Failure extends Message
{
	@Override
	public void perform()
	{
		System.out.println("Unable to perform request.");
	}
}
