package org.cnv.shr.trck;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class Comment implements TrackObject
{
	private String originIdent;
	private String destIdent;
	private String text;
	private int rating;
	private long date;

	public Comment(String originIdent, String destIdent, String text, int rating, long date)
	{
		this.originIdent = originIdent;
		this.destIdent = destIdent;
		this.text = text;
		this.rating = rating;
		this.date = date;
	}

	public Comment() {}

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
				case "oid":  originIdent = parser.getString();  break;
				case "did":  destIdent   = parser.getString();  break;
				case "text": text        = parser.getString();  break;
				}
				break;
			case VALUE_NUMBER:
				if (key == null) break;
				BigDecimal bd = new BigDecimal(parser.getString());
				switch (key)
				{
				case "rating": rating = bd.intValue(); break;
				case "date":   date   = bd.longValue(); break;
				}
				break;
			case END_OBJECT:
				return;
			default:
			}
		}
	}

	@Override
	public void print(JsonGenerator generator)
	{
		generator.writeStartObject();
		generator.write("oid", originIdent);
		generator.write("did", destIdent);
		generator.write("text", text);
		generator.write("rating", rating);
		generator.write("date", date);
		generator.writeEnd();
	}
	

	public void set(String originIdent, String destIdent, String text, int rating, long date)
	{
		this.originIdent = originIdent;
		this.destIdent = destIdent;
		this.text = text;
		this.rating = rating;
		this.date = date;
	}
	
	public String toString()
	{
		return TrackObjectUtils.toString(this);
	}
}
