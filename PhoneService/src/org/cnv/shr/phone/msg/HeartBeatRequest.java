package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.Services;


public class HeartBeatRequest extends PhoneMessage
{
	public HeartBeatRequest(ConnectionParams params)
	{
		super(params);
	}
	
	@Override
	public void perform(PhoneLine line, MsgHandler listener)
	{
		listener.onHeartBeatReq(line, this);
	}
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			}                                      
		}                                        
	}                                          
	public static String getJsonName() { return "HeartBeatRequest"; }
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
