package org.cnv.shr.json;

import java.security.PublicKey;
import java.util.HashMap;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.Misc;

public class JsonMap extends HashMap<PublicKey, byte[]> implements Jsonable
{
	public JsonMap() {}
	
	public JsonMap(JsonParser parser)
	{
		parse(parser);
	}

	public void generate(JsonGenerator generator)
	{
		generator.writeStartArray();
		for (java.util.Map.Entry<PublicKey, byte[]> entry : entrySet())
		{
			generator.write(KeyPairObject.serialize(entry.getKey()), Misc.format(entry.getValue()));
		}
		generator.writeEnd();
	}

	public void parse(JsonParser parser)
	{
		clear();
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case END_ARRAY:
				return;
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				put(KeyPairObject.deSerializePublicKey(key), Misc.format(parser.getString()));
			default:
				break;
			}
		}
	}
}
