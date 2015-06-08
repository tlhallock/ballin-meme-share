package org.cnv.shr.json;

import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.util.Jsonable;

public class JsonList<T extends Jsonable> extends LinkedList<T> 
{
	private String className;
	
	public JsonList(String name)
	{
		this.className = name;
	}

	public void generate(JsonGenerator generator)
	{
//		generator.writeStartArray();
		
		for (T t : this)
		{
			t.generate(generator);
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
			case START_OBJECT:
				add((T) JsonAllocators.create(className, parser));
				break;
			case END_ARRAY:
				return;
			}
		}
	}
}
