package org.cnv.shr.trck;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;

import org.cnv.shr.util.Jsonable;

public abstract class TrackObject implements Jsonable 
{
	public String toString()
	{
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(writer);)
		{
			generate(generator, null);
		}
		return new String(writer.toByteArray());
	}

	public void generate(JsonGenerator generator)
	{
		generate(generator, null);
	}
}
