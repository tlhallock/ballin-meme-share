package org.cnv.shr.util;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public interface Jsonable
{
	public void generate(JsonGenerator generator, String key);
	public void parse(JsonParser parser);
}
