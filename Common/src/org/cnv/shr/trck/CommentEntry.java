package org.cnv.shr.trck;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class CommentEntry implements TrackObject
{
	private static final int MAX_TEXT_LENGTH = 1024;
	private static final int MAX_RATING = 10;
	
	private String originIdent;
	private String destIdent;
	private String text;
	private int rating;
	private long date;

	public CommentEntry(String originIdent, String destIdent, String text, int rating, long date)
	{
		this.originIdent = originIdent;
		this.destIdent = destIdent;
		this.text = text;
		this.rating = rating;
		this.date = date;
	}

	public CommentEntry() {}

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
		generator.write("text", getText());
		generator.write("rating", getRating());
		generator.write("date", getDate());
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

	public String getOrigin()
	{
		return originIdent;
	}

	public String getDestination()
	{
		return destIdent;
	}

	public long getDate()
	{
		return Math.min(System.currentTimeMillis(), date);
	}

	public int getRating()
	{
		return Math.max(0, Math.min(MAX_RATING, rating));
	}

	public String getText()
	{
		if (text.length() > MAX_TEXT_LENGTH)
		{
			text = text.substring(0, MAX_TEXT_LENGTH);
		}
		return text;
	}

	public void setOrigin(String identifer)
	{
		this.originIdent = identifer;
	}

	
	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("originIdent", originIdent);
		generator.write("destIdent", destIdent);
		generator.write("text", text);
		generator.write("rating", rating);
		generator.write("date", date);
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
			switch(key) {
			case "originIdent":
				originIdent = parser.getString();
				break;
			case "destIdent":
				destIdent = parser.getString();
				break;
			case "text":
				text = parser.getString();
				break;
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			switch(key) {
			case "rating":
				rating = new BigDecimal(parser.getString()).intValue();
				break;
			case "date":
				date = new BigDecimal(parser.getString()).longValue();
				break;
			}
			break;
			default: break;
			}
		}
	}
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
