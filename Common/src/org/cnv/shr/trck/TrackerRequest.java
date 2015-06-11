package org.cnv.shr.trck;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class TrackerRequest extends TrackObject
{
	private String action;
	private HashMap<String, String> params;

	public TrackerRequest()
	{
		params = new HashMap<>();
	}
	
	public TrackerRequest(TrackerAction action)
	{
		this();
		this.action = action.name();
	}

	public TrackerRequest(JsonParser parser)
	{
		this();
		parse(parser);
	}
	
	public void setParameter(String name, String value)
	{
		params.put(name, value);
	}

	public void set(TrackerAction action)
	{
		this.action = action.name();
		params.clear();
	}

	public TrackerAction getAction()
	{
		return TrackerAction.valueOf(action);
	}

	public String getParam(String string)
	{
		return params.get(string);
	}
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		if (action!=null)
		generator.write("action", action);
		for (Entry<String, String> entry : params.entrySet())
		{
			generator.write(entry.getKey(), entry.getValue());
		}
		generator.writeEnd();
	}
	
	@Override                                    
	public void parse(JsonParser parser)
	{
		boolean needsaction = true;
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				if (key == null)
					break;
				switch (key)
				{
				case "action":
					action = parser.getString();
					break;
				default:
					params.put(key, parser.getString());
					break;
				}
				break;
			case END_OBJECT:
				if (needsaction)
				{
					throw new RuntimeException("Message needs action");
				}
				return;
			default:
			}
		}
	}
	public static String getJsonName() { return "TrackerRequest"; }
}
