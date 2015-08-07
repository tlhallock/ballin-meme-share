package org.cnv.shr.ports;

class Address
{
	public final String address;
	public final int port;
	
	public Address(String address, int port)
	{
		this.address = address;
		this.port = port;
	}
	
	public String toString()
	{
		return address + ":" + port;
	}
	
	public int hashCode()
	{
		return toString().hashCode();
	}
	
	public boolean equals(Object other)
	{
		if (!(other instanceof Address))
		{
			return false;
		}
		Address o = (Address) other;
		return address.equals(o.address) && port == o.port;
	}

	public int getPort()
	{
		return port;
	}
	public String getIp()
	{
		return address;
	}
}
