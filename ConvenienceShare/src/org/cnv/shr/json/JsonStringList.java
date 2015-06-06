package org.cnv.shr.json;

import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

public class JsonStringList extends LinkedList<String> 
{
	public JsonStringList()
	{
	}

	public void generate(JsonGenerator generator)
	{
		generator.writeStartArray();
		
		for (String t : this)
		{
			generator.write(t);
		}
		
		generator.writeEnd();
	}

	public void parse(JsonParser parser)
	{
		clear();
		while (parser.hasNext())
		{
			Event next = parser.next();
			switch (next)
			{
			case VALUE_STRING:
				add(parser.getString());
				break;
			case END_ARRAY:
				return;
			}
		}
	}
}
