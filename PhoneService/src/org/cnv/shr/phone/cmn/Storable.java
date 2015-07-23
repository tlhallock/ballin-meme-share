package org.cnv.shr.phone.cmn;

import java.io.IOException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public interface Storable
{
	public void generate(JsonGenerator generator, String key);
	public void parse(JsonParser parser) throws IOException;
}
