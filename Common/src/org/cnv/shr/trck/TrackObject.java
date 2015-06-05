package org.cnv.shr.trck;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.util.Jsonable;

public interface TrackObject extends Jsonable
{
	void read(JsonParser reader);
	void print(JsonGenerator generator);
}
