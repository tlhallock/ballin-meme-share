package org.cnv.shr.trck;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class Done implements TrackObject
{
	@Override
	public void read(JsonParser parser)
	{
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
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
		generator.writeEnd();
	}
}
