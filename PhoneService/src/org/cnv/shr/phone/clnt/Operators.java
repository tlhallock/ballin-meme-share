package org.cnv.shr.phone.clnt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.Services;

public class Operators
{
	private Path path;
	private LinkedList<OperatorInfo> operators = new LinkedList<>();
	
	public Operators(Path p)
	{
		this.path = p;
		load();
	}
	
	public void add(OperatorInfo info)
	{
		operators.add(info);
		save();
	}
	
	public void save()
	{
		try (OutputStream output = Files.newOutputStream(path);
				 JsonGenerator generator = Services.createGenerator(output, true);)
		{
			generator.writeStartArray();
			for (OperatorInfo info : operators)
			{
				info.generate(generator, null);
			}
			generator.writeEnd();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void load()
	{
		try (InputStream input = Files.newInputStream(path);
				 JsonParser parser = Services.createParser(input);)
		{
			while (parser.hasNext())
			{
				switch (parser.next())
				{
				case START_OBJECT:
					operators.add(new OperatorInfo(parser));
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
			save();
		}
	}
}
