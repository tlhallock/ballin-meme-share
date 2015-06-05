package org.cnv.shr.msg.key;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class MsgList
{
	public MsgList(JsonParser parser)
	{
		parse(parser);
	}

	public void generate(JsonGenerator generator)
	{
		generator.writeStartObject();
		generator.writeEnd();
	}

	public void parse(JsonParser parser)
	{
	}
}
