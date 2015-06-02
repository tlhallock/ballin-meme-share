package org.cnv.shr.trck;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class TrackerEntry implements TrackObject
{
	private String url;
	private int begin;
	private int end;
	
	public TrackerEntry(String url, int portB, int portE)
	{
		this.url = url;
		this.begin = portB;
		this.end = portE;
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
		generator.writeEnd();
	}
	
	public void set(String url, int begin, int end)
	{
		this.url = url;
		this.begin = begin;
		this.end = end;
	}
	
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
}
