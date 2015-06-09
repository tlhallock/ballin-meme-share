package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

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
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
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
			LogWrapper.getLogger().info("Bad message: too long.");
			return;
		}
		Machine machine = connection.getMachine();
		if (machine == null)
		{
			LogWrapper.getLogger().info("Bad message: no machine.");
			return;
		}
		if (!machine.getAllowsMessages())
		{
			LogWrapper.getLogger().info("Not accepting messages from " + machine.getName());
			return;
		}
		UserMessage message = new UserMessage(machine, type, messageStr);
		if (message.checkInsane())
		{
			return;
		}
		message.tryToSave();
		Services.notifications.messageReceived(message);
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("User message, type=").append(type).append(" msg=").append(messageStr);
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("type", type);
		if (messageStr!=null)
		generator.write("messageStr", messageStr);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsmessageStr = true;
		boolean needstype = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsmessageStr)
				{
					throw new RuntimeException("Message needs messageStr");
				}
				if (needstype)
				{
					throw new RuntimeException("Message needs type");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("messageStr")) {
				needsmessageStr = false;
				messageStr = parser.getString();
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("type")) {
				needstype = false;
				type = Integer.parseInt(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "UserMessageMessage"; }
	public UserMessageMessage(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
