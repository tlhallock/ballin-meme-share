package org.cnv.shr.json;

import java.util.HashSet;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class JsonStringSet extends HashSet<String>
{
	public void generate(JsonGenerator generator)
	{
		// generator.writeStartArray();

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
			switch (parser.next())
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
