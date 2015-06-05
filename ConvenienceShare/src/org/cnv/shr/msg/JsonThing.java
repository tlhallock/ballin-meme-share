package org.cnv.shr.msg;

import java.security.PublicKey;

import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.util.KeyPairObject;

public class JsonThing
{
	public static SharingState readSharingState(JsonParser parser)
	{
		if (!parser.next().equals(JsonParser.Event.VALUE_NUMBER))
		{
			// bad stuff...
		}
		return SharingState.valueOf(parser.getString());
	}

	public static SharedFileId readFileId(JsonParser parser)
	{
		return null;
	}

	public static Chunk readChunk(JsonParser parser)
	{
		return null;
	}

	public static PublicKey readKey(JsonParser parser)
	{
		if (!parser.next().equals(JsonParser.Event.VALUE_STRING))
		{
			// bad stuff...
		}
		return KeyPairObject.deSerializePublicKey(parser.getString());
	}
}
