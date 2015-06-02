package org.cnv.shr.trck;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class TrackerRequest implements TrackObject
{
	private TrackerAction action;
	private HashMap<String, String> params;

	public TrackerRequest()
	{
		params = new HashMap<>();
	}
	
	public TrackerRequest(TrackerAction action)
	{
		this.action = action;
		params = new HashMap<>();
	}
	
	public void setParameter(String name, String value)
	{
		params.put(name, value);
	}

	@Override
	public void read(JsonParser parser)
	{
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString(); break;
			case VALUE_STRING:
				if (key == null) break;
				switch (key)
				{
				case "action": action = TrackerAction.valueOf(parser.getString()); break;
				default:      params.put(key, parser.getString());                 break;
				}
				break;
			case END_OBJECT:
				return;
			default:
			}
		}
	}

	@Override
	public void print(JsonGenerator generator)
	{
		generator.writeStartObject();
		generator.write("action", action.name());
		for (Entry<String, String> entry : params.entrySet())
		{
			generator.write(entry.getKey(), entry.getValue());
		}
		generator.writeEnd();
	}

	public void set(TrackerAction action)
	{
		this.action = action;
		params.clear();
	}

	public TrackerAction getAction()
	{
		return action;
	}

	public String getParam(String string)
	{
		return params.get(string);
	}
}
