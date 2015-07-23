package org.cnv.shr.phone.msg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.Serializer;
import org.cnv.shr.phone.cmn.Storable;

public class JsonBinaryData implements Storable
{
	private Path outputPath;
	private InputStream input;
	
	JsonBinaryData(InputStream input)
	{
		this.input = input;
	}
	
	public JsonBinaryData(Path outputDir)
	{
		outputPath = outputDir;
	}

	@Override
	public void generate(JsonGenerator generator, String key)
	{
		Serializer.serialize(input, generator);
	}

	@Override
	public void parse(JsonParser parser) throws IOException
	{
		try (OutputStream newOutputStream = Files.newOutputStream(outputPath);)
		{
			Serializer.deSerialize(parser, newOutputStream);
		}
	}
}
