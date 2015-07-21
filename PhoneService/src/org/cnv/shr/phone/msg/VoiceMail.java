package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumber;
import org.cnv.shr.phone.cmn.Services;

public class VoiceMail extends PhoneMessage
{
	private long replyTime;
	private PhoneNumber replyNumber;
	
	public VoiceMail(ConnectionParams params)
	{
		super(params);
	}
	
	public VoiceMail(ConnectionParams params, InputStream input)
	{
		super(params);
	}

	@Override
	public void perform(PhoneLine line, MsgHandler listener)
	{
		
	}

	public PhoneNumber getReplyNumber()
	{
		return replyNumber;
	}

	public boolean pleaseReply()
	{
		return replyTime > System.currentTimeMillis();
	}

	public boolean hasData()
	{
		// TODO Auto-generated method stub
		return false;
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("reply", replyTime);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsReply = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsReply)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs reply");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case VALUE_FALSE:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("reply")) {
					needsReply = false;
					replyTime = false;
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_TRUE:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("reply")) {
					needsReply = false;
					replyTime = true;
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "VoiceMail"; }
	public String getJsonKey() { return getJsonName(); }
	public String toDebugString() {                                                      
		ByteArrayOutputStream output = new ByteArrayOutputStream();                        
		try (JsonGenerator generator = Services.createGenerator(output, true);)         {
			generate(generator, null);                                                       
		}                                                                                  
		return new String(output.toByteArray());                                           
	}                                                                                    
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
