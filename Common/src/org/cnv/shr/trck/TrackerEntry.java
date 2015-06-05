package org.cnv.shr.trck;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class TrackerEntry implements TrackObject
{
	private String url;
	private int begin;
	private int end;
	public static int TRACKER_PORT_END   = 9005;
	public static int TRACKER_PORT_BEGIN = 9001;
        public static int MACHINE_PAGE_SIZE  = 50;
	
	private Boolean sync;
	
	public TrackerEntry(String url, int portB, int portE)
	{
		this.url = url;
		this.begin = portB;
		this.end = portE;
	}
	
	public TrackerEntry(TrackerEntry entry)
	{
		this.url = entry.url;
		this.begin = entry.begin;
		this.end = entry.end;
	}

	public TrackerEntry() {}

	@Override
	public void read(JsonParser parser)
	{
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				if (key == null) break;
				switch (key)
				{
				case "url":    url      = parser.getString();  break;
				}
				break;
			case VALUE_NUMBER:
				if (key == null) break;
				BigDecimal bd = new BigDecimal(parser.getString());
				switch (key)
				{
				case "beginPort": begin = bd.intValue(); break;
				case "endPort":   end   = bd.intValue(); break;
				}
				break;
			case VALUE_FALSE:
				switch(key)
				{
				case "sync" : sync = false; break;
				}
				break;
			case VALUE_TRUE:
				switch(key)
				{
				case "sync" : sync = true; break;
				}
				break;
			case END_OBJECT:
				return;
			default:
				break;
			}
		}
	}

	@Override
	public void print(JsonGenerator generator)
	{
		generator.writeStartObject();
		generator.write("url", url);
		generator.write("beginPort", begin);
		generator.write("endPort", end);
		if (sync != null)
			generator.write("sync", sync);
		generator.writeEnd();
	}
	
	public void set(String url, int begin, int end)
	{
		this.url = url;
		this.begin = begin;
		this.end = end;
	}
	
	@Override
	public String toString()
	{
		return TrackObjectUtils.toString(this);
	}

	public String getIp()
	{
		return url;
	}

	public int getBeginPort()
	{
		return begin;
	}
	
	public int getEndPort()
	{
		return end;
	}

	public String getAddress()
	{
		return url + ":" + begin + "-" + end;
	}
	
	public boolean shouldSync()
	{
		return sync != null && sync;
	}
	
	public void setSync(boolean val)
	{
		sync = val;
	}

	
	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("url", url);
		generator.write("begin", begin);
		generator.write("end", end);
		generator.writeEnd();
	}

	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("url")) {
				url = parser.getString();
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "begin":
				begin = new BigDecimal(parser.getString()).intValue();
				break;
			case "end":
				end = new BigDecimal(parser.getString()).intValue();
				break;
			}
			break;
			default: break;
			}
		}
	}
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
