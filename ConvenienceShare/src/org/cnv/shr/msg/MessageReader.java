package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.util.HashMap;

import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.dwn.ChunkList;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.ChunkResponse;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.DownloadDone;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.msg.dwn.RequestCompletionStatus;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.ConnectionOpened;
import org.cnv.shr.msg.key.InitiateAuthentication;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.KeyFailure;
import org.cnv.shr.msg.key.KeyNotFound;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.msg.key.RevokeKey;
import org.cnv.shr.util.ByteReader;



public class MessageReader
{
	private HashMap<Integer, MessageIdentifier> identifiers = new HashMap<>();
	
	public MessageReader()
	{
		add(new MessageIdentifier(ChunkList.class                  ));
		add(new MessageIdentifier(DownloadDone.class               ));
		add(new MessageIdentifier(CompletionStatus.class           ));
		add(new MessageIdentifier(FileRequest.class                ));
		add(new MessageIdentifier(ChunkResponse.class              ));
		add(new MessageIdentifier(RequestCompletionStatus.class    ));
		add(new MessageIdentifier(ChunkRequest.class               ));
		add(new MessageIdentifier(MachineHasFile.class             ));
		add(new MessageIdentifier(ListRoots.class                  ));
		add(new MessageIdentifier(ListPath.class                   ));
		add(new MessageIdentifier(MachineFound.class               ));
		add(new MessageIdentifier(UpdateCode.class                 ));
		add(new MessageIdentifier(DoneMessage.class                ));
		add(new MessageIdentifier(DirectoryList.class              ));
		add(new MessageIdentifier(RootList.class                   ));
		add(new MessageIdentifier(FindMachines.class               ));
		add(new MessageIdentifier(RequestAccess.class              ));
		add(new MessageIdentifier(Failure.class                    ));
		add(new MessageIdentifier(Wait.class                       ));
		add(new MessageIdentifier(HeartBeat.class                  ));
		add(new MessageIdentifier(LookingFor.class                 ));
		add(new MessageIdentifier(ConnectionOpenAwk.class          ));
		add(new MessageIdentifier(NewKey.class                     ));
		add(new MessageIdentifier(RevokeKey.class                  ));
		add(new MessageIdentifier(ConnectionOpened.class           ));
		add(new MessageIdentifier(KeyNotFound.class                ));
		add(new MessageIdentifier(KeyFailure.class                 ));
		add(new MessageIdentifier(InitiateAuthentication.class     ));
		add(new MessageIdentifier(KeyChange.class                  ));

		Services.logger.logStream.println("Message map:\n" + this);
	}
	
	private void add(MessageIdentifier identifier)
	{
		int type = identifier.getType();
		if (type > Byte.MAX_VALUE || type < Byte.MIN_VALUE)
		{
			Services.logger.logStream.println("Message type " + type + " for " + identifier.name + " is not in range.");
			Services.logger.logStream.println(this);
			Main.quit();
			return;
		}
		MessageIdentifier messageIdentifier = identifiers.get(type);
		if (messageIdentifier != null)
		{
			Services.logger.logStream.println("Type " + type + " is already used by " + messageIdentifier.name + " so " + identifier.name + " cannot also use it.");
			Services.logger.logStream.println(this);
			Main.quit();
			return;
		}
		identifiers.put(type, identifier);
	}
	
	public Message readMsg(InetAddress address, Communication c) throws IOException
	{
		int msgType = ByteReader.readInt(c.getIn());

		MessageIdentifier messageIdentifier = identifiers.get(msgType);
		if (messageIdentifier == null)
		{
			Services.logger.logStream.println("Ignoring unkown message type: " + msgType + " from " + address);
			return null;
		}
		
		Message message = messageIdentifier.create(address, c.getIn());

		if (!c.authenticate(message))
		{
			Services.logger.logStream.println("Unable to authenticate message of type " + msgType + " from " + address);
			return null;
		}
		
		message.read(c.getIn());
		
		System.out.println("Msg: " + message);

		return message;
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		for (MessageIdentifier identifier : identifiers.values())
		{
			builder.append(identifier).append('\n');
		}
		return builder.toString();
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
				Services.logger.logStream.println("Received message of type " + name);
				return constructor.newInstance(address, stream);
			}
			catch (Exception e)
			{
				Services.logger.logStream.println("Unable to create message type "  + name);
				e.printStackTrace(Services.logger.logStream);
				Main.quit();
				return null;
			}
		}
		
		public String toString()
		{
			return type + "->" + name;
		}
	}
}
