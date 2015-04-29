package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.HashMap;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;

public class MessageReader
{
	private HashMap<Integer, MessageIdentifier> identifiers = new HashMap<>();
	
	public MessageReader()
	{
		add(new MessageIdentifier(MachineFound.class));
	}
	
	private void add(MessageIdentifier identifier)
	{
		MessageIdentifier messageIdentifier = identifiers.get(identifier.getType());
		if (messageIdentifier != null)
		{
			Services.logger.logStream.println("Type " + identifier.getType() + " is already used by " + messageIdentifier.name + " so " + identifier.name + " cannot also use it.");
			Main.quit();
			return;
		}
		identifiers.put(identifier.getType(), identifier);
	}
	
	public Message readMsg(InetAddress address, InputStream inputStream) throws IOException
	{
		int msgType = inputStream.read();

		MessageIdentifier messageIdentifier = identifiers.get(msgType);
		if (messageIdentifier == null)
		{
			Services.logger.logStream.println("Ignoring unkown message type: " + msgType + " from " + address);
			return null;
		}
		
		Message message = messageIdentifier.create(address, inputStream);

		if (!message.authenticate())
		{
			Services.logger.logStream.println("Unable to authenticate message of type " + msgType + " from " + address);
			return null;
		}
		
		message.read(inputStream);

		return message;
	}
	
	
	private class MessageIdentifier
	{
		String name;
		int type;
		Constructor<? extends Message> constructor;
		
		MessageIdentifier(Class<? extends Message> c)
		{
			try
			{
				name = c.getName();
				type = c.getField("TYPE").getInt(null);
				constructor = c.getConstructor(InetAddress.class, InputStream.class);
				if (constructor == null)
				{
					throw new Exception("Missing constructor in class " + name);
				}
			}
			catch (Exception e)
			{
				Services.logger.logStream.println("Unable to read fields for class " + name);
				e.printStackTrace(Services.logger.logStream);
				Main.quit();
			}
		}
		
		int getType()
		{
			return type;
		}
		
		Message create(InetAddress address, InputStream stream)
		{
			try
			{
				return constructor.newInstance(address, stream);
			}
			catch (Exception e)
			{
				Services.logger.logStream.println("Unable to create message type "  + name);
				Main.quit();
				return null;
			}
		}
	}
}
