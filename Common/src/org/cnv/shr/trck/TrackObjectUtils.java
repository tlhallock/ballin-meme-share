package org.cnv.shr.trck;

import java.io.InputStream;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParserFactory;

import org.cnv.shr.util.LogWrapper;

public class TrackObjectUtils
{
	public static final JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(null);
	public static final JsonParserFactory parserFactory = Json.createParserFactory(null);
	
	static String toString(TrackObject machineEntry)
	{
		StringWriter writer = new StringWriter();
		try (JsonGenerator generator = generatorFactory.createGenerator(writer);)
		{
			machineEntry.print(generator);
		}
		return writer.toString();
	}

	public static JsonParser openArray(InputStream input) throws JsonException
	{
		JsonParser parser = parserFactory.createParser(input);
		Event event = parser.next();
		if (!event.equals(JsonParser.Event.START_ARRAY))
		{
			throw new JsonException("Unexpected json event: " + event);
		}
		return parser;
	}
	
	public static <T extends TrackObject> boolean next(JsonParser parser, T t)
	{
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case START_OBJECT:
				t.read(parser);
				return true;
			case END_ARRAY:
				return false;
			default:
				// ignore
			}
		}
		return false;
	}
	
	public static void debug(InputStream input)
	{
		JsonParser openArray = openArray(input);
		MachineEntry entry = new MachineEntry();
		while (next(openArray, entry))
		{
			System.out.println(entry);
		}
	}
	
	public static <T extends TrackObject> boolean read(JsonParser parser, T t)
	{
		Event next = parser.next();
		if (!next.equals(JsonParser.Event.START_OBJECT))
		{
			LogWrapper.getLogger().info("Expected start object for " + t.getClass().getName() + ", found: " + next);
			return false;
		}
		t.read(parser);
		return true;
	}
}
