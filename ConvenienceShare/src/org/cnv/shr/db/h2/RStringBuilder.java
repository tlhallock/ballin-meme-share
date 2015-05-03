package org.cnv.shr.db.h2;

public class RStringBuilder
{
	// Hack for now...
	String builder = "";
	
	public RStringBuilder preppend(String s)
	{
		builder = s + builder;
		return this;
	}
	
	public String toString()
	{
		return builder.toString();
	}
}
