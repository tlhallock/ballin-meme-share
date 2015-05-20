package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class UserMessageMessage extends Message
{
	public static int TYPE = 7;
        
        private int type;
        private String messageStr;
	
	public UserMessageMessage(UserMessage m)
	{
            this.type = m.getType();
            this.messageStr = m.getMessage();
	}

	public UserMessageMessage(InputStream i) throws IOException
	{
		super(i);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
        
	@Override
	protected void parse(ByteReader reader) throws IOException
	{
            type = reader.readInt();
            messageStr = reader.readString();
	}
        
	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
	{
		if (messageStr.length() > UserMessage.MAX_MESSAGE_LENGTH)
		{
			messageStr = messageStr.substring(0, UserMessage.MAX_MESSAGE_LENGTH);
		}
		buffer.append(type);
		buffer.append(messageStr);
	}
	@Override
	public void perform(Communication connection) throws Exception
	{
		if (messageStr.length() > UserMessage.MAX_MESSAGE_LENGTH)
		{
			Services.logger.println("Bad message: too long.");
			return;
		}
		Machine machine = connection.getMachine();
		if (machine == null)
		{
			Services.logger.println("Bad message: no machine.");
			return;
		}
		if (!machine.getAllowsMessages())
		{
			Services.logger.println("Not accepting messages from " + machine.getName());
			return;
		}
		UserMessage message = new UserMessage(machine, type, messageStr);
		message.save();
		Services.notifications.messageReceived(message);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("User message, type=").append(type).append(" msg=").append(messageStr);
		return builder.toString();
	}
}
