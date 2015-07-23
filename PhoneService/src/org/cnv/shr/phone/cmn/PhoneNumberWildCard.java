package org.cnv.shr.phone.cmn;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Iterator;

import javax.json.JsonException;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class PhoneNumberWildCard implements Storable, Comparable<PhoneNumberWildCard>
{
	private static final String ANY = "*";
	
	private HashSet<String> ips = new HashSet<>();
	private HashSet<String> idents = new HashSet<>();
	
	public PhoneNumberWildCard(JsonParser parser)
	{
		parse(parser);
	}

	public PhoneNumberWildCard() {}

	public boolean matches(PhoneNumber number)
	{
		return 
				(idents.contains(ANY) || idents.contains(number.getIdent())) 
				&& 
				(ips.contains(ANY)    || ips.contains(number.getIp()      ));
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof PhoneNumberWildCard))
		{
			return false;
		}
		PhoneNumberWildCard other = (PhoneNumberWildCard) o;
		if (other.ips.size() != ips.size()) return false;
		if (other.idents.size() != idents.size()) return false;
		for (String ip : ips)
		{
			if (!other.ips.contains(ip)) return false;
		}
		for (String ip : idents)
		{
			if (!other.idents.contains(ip)) return false;
		}
		return true;
	}
	
	public PhoneNumber getNumber()
	{
		if (!hasNumber())
		{
			throw new RuntimeException("Unable to create number...");
		}
		return new PhoneNumber(idents.iterator().next(), ips.iterator().next());
	}

	public boolean hasNumber()
	{
		return ips.size() == 1 && idents.size() == 1 && !ips.contains(ANY) && !idents.contains(ANY);
	}

	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		{
			generator.writeStartArray("ips");
			for (String t : ips)
			{
				generator.write(t);
			}
			generator.writeEnd();
		}
		{
			generator.writeStartArray("idents");
			for (String t : idents)
			{
				generator.write(t);
			}
			generator.writeEnd();
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsIps = true;
		boolean needsIdents = true;
		outer:
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsIps)
				{
					throw new JsonException("phone number wild card needs ips");
				}
				if (needsIdents)
				{
					throw new JsonException("phone number wild card needs idents");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_ARRAY:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "ips":
					needsIps = false;
					ips.clear();
					while (parser.hasNext())
					{
						switch (parser.next())
						{
						case VALUE_STRING:
							ips.add(parser.getString());
							break;
						case END_ARRAY:
							continue outer;
						}
					}
				case "idents":
					needsIdents = false;
					idents.clear();
					while (parser.hasNext())
					{
						switch (parser.next())
						{
						case VALUE_STRING:
							idents.add(parser.getString());
							break;
						case END_ARRAY:
							continue outer;
						}
					}
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "PhoneNumberWildCard"; }
	public String getJsonKey() { return getJsonName(); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = Services.createGenerator(output, true);)         {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}

	public void addAnyIdent()
	{
		idents.add(ANY);
	}
	
	public void addAnyIp()
	{
		ips.add(ANY);
	}
	
	public void addIdent(String ident)
	{
		idents.add(ident);
	}

	public void addIp(String ip)
	{
		ips.add(ip);
	}

	@Override
	public int compareTo(PhoneNumberWildCard o)
	{
		int c;
		c = Integer.compare(ips.size(), o.ips.size());
		if (c != 0) return c;
		c = Integer.compare(idents.size(), o.idents.size());
		if (c != 0) return c;
		
		Iterator<String> iterator1 = ips.iterator();
		Iterator<String> iterator2 = o.ips.iterator();
		while (iterator1.hasNext())
		{
			c = iterator1.next().compareTo(iterator2.next());
			if (c!=0) return c;
		}
		iterator1 = idents.iterator();
		iterator2 = o.idents.iterator();
		while (iterator1.hasNext())
		{
			c = iterator1.next().compareTo(iterator2.next());
			if (c!=0) return c;
		}
		return 0;
	}                                                                                    


	
//	public static final class PortRange
//	{
//		public final int minPort;
//		public final int maxPort;
//		
//		public PortRange(int minPort, int maxPort)
//		{
//			this.minPort = minPort;
//			this.maxPort = maxPort;
//		}
//		public PortRange(JsonParser parser)
//		{
//			this.minPort = -1;
//			this.maxPort = -1;
//		}
//
//		public void generate(JsonGenerator generator)
//		{
//			generator.write("min", minPort);
//			generator.write("max", maxPort);
//		}
//	}
}
