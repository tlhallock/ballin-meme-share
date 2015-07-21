package org.cnv.shr.phone.cmn;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Hashtable;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class PhoneNumberWildCard implements Storable
{
	private Hashtable<String, PortRange> ips = new Hashtable<>();
	private HashSet<String> idents = new HashSet<>();
	
	public PhoneNumberWildCard(JsonParser parser)
	{
		parse(parser);
	}

	public boolean matches(InetAddress address)
	{
		return false;
	}
	
	public boolean matches(String ident)
	{
		return false;
	}

	@Override
	public void generate(JsonGenerator generator, String key)
	{
		
	}

	@Override
	public void parse(JsonParser parser)
	{
		// TODO Auto-generated method stub
		
	}
	
	public static final class PortRange
	{
		public final int minPort;
		public final int maxPort;
		
		public PortRange(int minPort, int maxPort)
		{
			this.minPort = minPort;
			this.maxPort = maxPort;
		}
		public PortRange(JsonParser parser)
		{
			this.minPort = -1;
			this.maxPort = -1;
		}

		public void generate(JsonGenerator generator)
		{
			generator.write("min", minPort);
			generator.write("max", maxPort);
		}
	}
}
