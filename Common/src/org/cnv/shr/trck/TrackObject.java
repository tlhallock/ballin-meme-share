package org.cnv.shr.trck;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public interface TrackObject
{
	void read(JsonParser reader);
	void print(JsonGenerator generator);
}
