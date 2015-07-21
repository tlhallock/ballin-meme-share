package org.cnv.shr.phone.cmn;

import java.util.HashMap;

import javax.json.stream.JsonParser;

import org.cnv.shr.phone.msg.ClientInfo;
import org.cnv.shr.phone.msg.Dial;
import org.cnv.shr.phone.msg.Hangup;
import org.cnv.shr.phone.msg.HeartBeatRequest;
import org.cnv.shr.phone.msg.HeartBeatResponse;
import org.cnv.shr.phone.msg.NoMoreMessages;
import org.cnv.shr.phone.msg.PhoneMessage;
import org.cnv.shr.phone.msg.PhoneRing;
import org.cnv.shr.phone.msg.VoiceMail;

public class MetaHandler
{
	HashMap<String, Allocator> allocators = new HashMap<>();
	
	private abstract class Allocator
	{
		abstract PhoneMessage create(ConnectionParams parameters);
	}
	
	public MetaHandler()
	{
		allocators.put(Dial.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new Dial(parameters);
			}});

		allocators.put(NoMoreMessages.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new NoMoreMessages(parameters);
			}});

		allocators.put(Hangup.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new Hangup(parameters);
			}});

		allocators.put(HeartBeatRequest.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new HeartBeatRequest(parameters);
			}});

		allocators.put(HeartBeatResponse.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new HeartBeatResponse(parameters);
			}});

		allocators.put(PhoneRing.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new PhoneRing(parameters);
			}});

		allocators.put(VoiceMail.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new VoiceMail(parameters);
			}});

		allocators.put(ClientInfo.getJsonName(), new Allocator() {
			@Override
			PhoneMessage create(ConnectionParams parameters)
			{
				return new ClientInfo(parameters);
			}});
		
		
		
		
	}

	
	
	public PhoneMessage parse(JsonParser parser, ConnectionParams parameters)
	{
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case START_OBJECT:
				if (key == null)
				{
					throw new RuntimeException("Value with no key!");
				}
				Allocator allocator = allocators.get(key);
				if (allocator == null)
				{
					throw new RuntimeException("Unkown message type!");
				}
				PhoneMessage create = allocator.create(parameters);
				create.parse(parser);
				return create;
			default:
				System.out.println("Unknown type found in message: " + e);
			}
		}
		return null;
	}
}
