package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumber;
import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.cmn.Services;

public class VoiceMail extends PhoneMessage
{
	private long replyTime;
	private PhoneNumber sourceNumber;
	private PhoneNumberWildCard destinationNumber;
	private boolean hasData;
	
	@MyParserIgnore
	private Path outputDir;
	
	@MyParserNullable
	JsonBinaryData data;
	
	public VoiceMail(long replyTime, PhoneNumberWildCard destinationNumber)
	{
		this.replyTime = replyTime;
		this.destinationNumber = destinationNumber;
		hasData = false;
	}
	
	public VoiceMail(ConnectionParams params)
	{
		super(params);
		outputDir = ((Path)params.get(ConnectionParams.voicemailPath)).resolve("voicemail." + System.currentTimeMillis() + Math.random());
		data = new JsonBinaryData(outputDir);
	}

	public VoiceMail(InputStream input)
	{
		replyTime = -1;
		hasData = true;
		data = new JsonBinaryData(input);
	}
	
	public Path getDataPath()
	{
		if (!hasData)
		{
			return null;
		}
		
		return outputDir;
	}
	
	public long getReplyTime()
	{
		return replyTime;
	}

	@Override
	public void perform(PhoneLine line, MsgHandler listener)
	{
		listener.onVoicemail(line, this);
	}

	public PhoneNumber getSourceNumber()
	{
		return sourceNumber;
	}
	public PhoneNumberWildCard getDestinationNumber()
	{
		return destinationNumber;
	}

	public boolean pleaseReply()
	{
		return replyTime > 0;
	}

	public boolean hasData()
	{
		return false;
	}
	
	// todo writedata

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("replyTime", replyTime);
		sourceNumber.generate(generator, "sourceNumber");
		destinationNumber.generate(generator, "destinationNumber");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsSourceNumber = true;
		boolean needsDestinationNumber = true;
		boolean needsReplyTime = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsSourceNumber)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.msg.VoiceMail\" needs \"sourceNumber\"");
				}
				if (needsDestinationNumber)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.msg.VoiceMail\" needs \"destinationNumber\"");
				}
				if (needsReplyTime)
				{
					throw new javax.json.JsonException("Incomplete json: type=\"org.cnv.shr.phone.msg.VoiceMail\" needs \"replyTime\"");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_OBJECT:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				switch(key) {
				case "sourceNumber":
					needsSourceNumber = false;
					sourceNumber = new org.cnv.shr.phone.cmn.PhoneNumber(parser);
					break;
				case "destinationNumber":
					needsDestinationNumber = false;
					destinationNumber = new org.cnv.shr.phone.cmn.PhoneNumberWildCard(parser);
					break;
				default: Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_NUMBER:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("replyTime")) {
					needsReplyTime = false;
					replyTime = Long.parseLong(parser.getString());
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
