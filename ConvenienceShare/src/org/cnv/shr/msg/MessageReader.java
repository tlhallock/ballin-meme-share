package org.cnv.shr.msg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.json.stream.JsonParser;

import org.cnv.shr.json.JsonAllocators;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;



public class MessageReader
{
	public Message readMsg(JsonParser input) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		String className = null;
		wh:
		while (input.hasNext())
		{
			JsonParser.Event e = input.next();
			switch (e)
			{
			case VALUE_STRING:
				className = input.getString();
				break;
			case START_OBJECT:
				break wh;
			}
		}

		LogWrapper.getLogger().info("Received message of type " + className);
		Jsonable create = JsonAllocators.create(className, input);
		if (create == null)
		{
			LogWrapper.getLogger().info("Ignoring unknown message type: " + className + " from ");
			return null;
		}
		if (!(create instanceof Message))
		{
			LogWrapper.getLogger().info("Received something that is not a message: " + className);
			return null;
		}
		LogWrapper.getLogger().info("Read " + create);
		return (Message) create;
	}
}
