
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.trck;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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
  private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final JsonGeneratorFactory generatorFactory = createGeneratorFactory();
	private static JsonGeneratorFactory createGeneratorFactory()
	{
    Map<String, Object> properties = new HashMap<>(1);
//    properties.put(JsonGenerator.PRETTY_PRINTING, true);
		return Json.createGeneratorFactory(properties);
	}
	private static final JsonParserFactory parserFactory = Json.createParserFactory(null);
	
	public static JsonGenerator createGenerator(OutputStream output)
	{
		return generatorFactory.createGenerator(output, UTF_8);
		
	}
	public static JsonParser createParser(InputStream input)
	{
		return parserFactory.createParser(input, UTF_8);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static JsonParser openArray(InputStream input) throws JsonException
	{
		JsonParser parser = parserFactory.createParser(input);
		return openArray(parser);
	}

	public static JsonParser openArray(JsonParser parser)
	{
		Event event = parser.next();
		if (!event.equals(JsonParser.Event.START_ARRAY))
		{
			throw new JsonException("Unexpected json event: " + event);
		}
		return parser;
	}

	public static <T extends TrackObject> boolean next(JsonParser parser, T t)
	{
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case START_OBJECT:
				t.parse(parser);
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
		t.parse(parser);
		return true;
	}
}
