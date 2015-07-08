package org.cnv.shr.json;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.json.JsonList.Allocator;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;

public class JsonSet<T extends Jsonable> extends LinkedList<T> 
{
	private Allocator<T> allocator;

	public JsonSet(Allocator<T> allocator)
	{
		this.allocator = allocator;
	}

	public void generate(JsonGenerator generator)
	{
//		generator.writeStartArray();
		
		for (T t : this)
		{
			t.generate(generator, null);
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
			case START_OBJECT:
				add(allocator.create(parser));
				break;
			case END_ARRAY:
				return;
			}
		}
	}
	
	public String toDebugString() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator);
		}
		return new String(output.toByteArray());
	}
}
