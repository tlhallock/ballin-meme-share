
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	private static final JsonGeneratorFactory generatorFactory = createGeneratorFactory(false);
	private static final JsonGeneratorFactory prettyGeneratorFactory = createGeneratorFactory(true);
	private static JsonGeneratorFactory createGeneratorFactory(boolean prettyPrinting)
	{
    Map<String, Object> properties = new HashMap<>(1);
    if (prettyPrinting)
    {
    	properties.put(JsonGenerator.PRETTY_PRINTING, true);
    }
		return Json.createGeneratorFactory(properties);
	}
	private static final JsonParserFactory parserFactory = Json.createParserFactory(null);
	
	public static JsonGenerator createGenerator(OutputStream output)
	{
		return createGenerator(output, false);
	}
	public static JsonGenerator createGenerator(OutputStream output, boolean prettyPrinting)
	{
		if (prettyPrinting)
		{
			return prettyGeneratorFactory.createGenerator(output, UTF_8);
		}
		return generatorFactory.createGenerator(output, UTF_8);
	}
	public static JsonParser createParser(InputStream input)
	{
		

		BufferedWriter logFile; 
		{ 
			try
			{
				String string = "log.jsonParser." + System.currentTimeMillis() + "." + Math.random() + ".txt";
				Path absolutePath = Paths.get(string).toAbsolutePath();
				Map<String, Object> properties = new HashMap<>(1);
				properties.put(JsonGenerator.PRETTY_PRINTING, true);
				System.out.println("Logging to " + absolutePath);
				logFile = Files.newBufferedWriter(absolutePath);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		
		// OMG, java is #!@%*&-up.
		// The default InputStreamReader will try to read past the end of what is available.
		// This results in stream hanging, even though it has bytes to return.
		// To fix this, we create our own parser that is 10x smarter.
		return parserFactory.createParser(new InputStreamReader(input, UTF_8)
		{
			public int read() throws IOException
			{
				int read = super.read();
				logFile.write(read); logFile.flush();
				return read;
			}
			@Override
			public int read(char[] cbuf, int offset, int length) throws IOException
			{
				int amountToRead = length;
				if (amountToRead > input.available())
				{
					amountToRead = input.available();
				}
				// We have to read 1 byte, because the json parser pukes otherwise.
				// This doesn't hurt, because we can block for 1 byte: we aren't waiting while we have something to return.
				if (amountToRead < 1)
				{
					amountToRead = 1;
				}
				int read = super.read(cbuf, offset, amountToRead);
				
				logFile.write(cbuf, offset, read); logFile.flush();
				
				if (read == 0)
				{
					LogWrapper.getLogger().severe("Read 0 byte from input!!!!");
				}
				
				return read;
			}
		});
//		return parserFactory.createParser(input, UTF_8);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public static JsonParser openArray(InputStream input) throws JsonException
	{
		return openArray(parserFactory.createParser(input));
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
		try (JsonParser openArray = openArray(input);)
		{
			MachineEntry entry = new MachineEntry();
			while (next(openArray, entry))
			{
				System.out.println(entry);
			}
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
