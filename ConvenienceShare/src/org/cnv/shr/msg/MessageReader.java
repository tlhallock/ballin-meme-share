package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.dwn.ChecksumRequest;
import org.cnv.shr.msg.dwn.ChecksumResponse;
import org.cnv.shr.msg.dwn.ChunkList;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.ChunkResponse;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.DownloadDone;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.msg.dwn.NewAesKey;
import org.cnv.shr.msg.dwn.RequestCompletionStatus;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.ConnectionOpened;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.KeyFailure;
import org.cnv.shr.msg.key.KeyNotFound;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.msg.key.OpenConnection;
import org.cnv.shr.msg.key.PermissionFailure;
import org.cnv.shr.msg.key.RevokeKey;
import org.cnv.shr.msg.key.WhoIAm;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;



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
		add(new MessageIdentifier(PathList.class                   ));
		add(new MessageIdentifier(RootList.class                   ));
		add(new MessageIdentifier(FindMachines.class               ));
		add(new MessageIdentifier(UserMessageMessage.class         ));
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
		add(new MessageIdentifier(OpenConnection.class             ));
		add(new MessageIdentifier(KeyChange.class                  ));
		add(new MessageIdentifier(WhoIAm.class                     ));
		add(new MessageIdentifier(EmptyMessage.class               ));
		add(new MessageIdentifier(DoneResponse.class               ));
		add(new MessageIdentifier(ChecksumRequest.class            ));
		add(new MessageIdentifier(ChecksumResponse.class           ));
		add(new MessageIdentifier(NewAesKey.class                  ));
		add(new MessageIdentifier(PermissionFailure.class          ));

		LogWrapper.getLogger().info("Message map:\n" + this);
	}
	
	private void add(MessageIdentifier identifier)
	{
		int type = identifier.getType();
		if (type > Byte.MAX_VALUE || type < Byte.MIN_VALUE)
		{
			LogWrapper.getLogger().severe("Message type " + type + " for " + identifier.name + " is not in range.");
			LogWrapper.getLogger().severe(this.toString());
			Services.quiter.quit();
			return;
		}
		MessageIdentifier messageIdentifier = identifiers.get(type);
		if (messageIdentifier != null)
		{
			LogWrapper.getLogger().severe("Type " + type + " is already used by " + messageIdentifier.name 
					+ " so " + identifier.name + " cannot also use it.");
			LogWrapper.getLogger().severe(this.toString());
			Services.quiter.quit();
			return;
		}
		identifiers.put(type, identifier);
	}
	
	public Message readMsg(ByteReader input) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		long msgTypeL = input.tryToReadInt();
		if (msgTypeL < 0)
		{
			return null;
		}
		int msgType = (int) msgTypeL;

		MessageIdentifier messageIdentifier = identifiers.get(msgType);
		if (messageIdentifier == null)
		{
			LogWrapper.getLogger().info("Ignoring unknown message type: " + msgType + " from ");
			return null;
		}
		
		return messageIdentifier.create(input);
	}
	
	@Override
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
				constructor = c.getConstructor(InputStream.class);
				if (constructor == null)
				{
					throw new Exception("Missing constructor in class " + name);
				}
			}
			catch (Exception e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to read fields for class " + name, e);
				Services.quiter.quit();
			}
		}
		
		int getType()
		{
			return type;
		}
		
		Message create(ByteReader stream) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException
		{
				LogWrapper.getLogger().info("Received message of type " + name);
				Message newInstance = constructor.newInstance(DUMMY_STREAM);
				newInstance.parse(stream);
				return newInstance;
//			catch (Exception e)
//			{
//				LogWrapper.getLogger().info("Unable to create message type "  + name);
//				LogWrapper.getLogger().log(Level.INFO, , e);
//				
////				Throwable t = e;
////				while (t != null)
////				{
////					t.printStackTrace(Services.logger.;
////					t = t.getCause();
////				}
//				
//				Main.quit();
//			}
		}
		
		@Override
		public String toString()
		{
			return type + "->" + name;
		}
	}
	private static final InputStream DUMMY_STREAM = new InputStream() {
		@Override
		public int read() throws IOException
		{
			return -1;
		}};
}
