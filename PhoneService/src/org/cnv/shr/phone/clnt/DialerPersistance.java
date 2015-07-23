package org.cnv.shr.phone.clnt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.Appointment;
import org.cnv.shr.phone.cmn.Services;

public class DialerPersistance
{
	private Path path;
	private HashSet<OperatorInfo> operators = new HashSet<>();
	DialerParams params = new DialerParams();
	LinkedList<Appointment> appointments = new LinkedList<>();
	
	public DialerPersistance(Path p)
	{
		this.path = p;
		load();
	}
	
	public void add(OperatorInfo info)
	{
		if (operators.add(info))
		{
			save();
		}
	}
	
	public void save()
	{
		try (OutputStream output = Files.newOutputStream(path);
				 JsonGenerator generator = Services.createGenerator(output, true);)
		{
			generator.writeStartObject();
			generator.writeStartArray("operators");
			for (OperatorInfo info : operators)
			{
				info.generate(generator, null);
			}
			generator.writeEnd();
			generator.writeStartArray("appointments");
			for (Appointment appointment : appointments)
			{
				appointment.generate(generator, null);
			}
			generator.writeEnd();
			params.generate(generator, "params");
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
			final int NOTHING = 0;
			final int READING_OPERATORS = 1;
			final int READING_PARAMS = 2;
			final int READING_APPOINTMENTS = 3;
			int state = NOTHING;
			
			while (parser.hasNext())
			{
				switch (parser.next())
				{
				case START_OBJECT:
					switch (state)
					{
					case NOTHING:
						continue;
					case READING_OPERATORS:
						operators.add(new OperatorInfo(parser));
						break;
					case READING_PARAMS:
						params.parse(parser);
						break;
					case READING_APPOINTMENTS:
						appointments.add(new Appointment(parser));
						break;
					}
					break;
				case KEY_NAME:
					switch(parser.getString())
					{
					case "operators":
						state = READING_OPERATORS;
						break;
					case "params":
						state = READING_PARAMS;
						break;
					case "appointments":
						state = READING_APPOINTMENTS;
						break;
					}
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
