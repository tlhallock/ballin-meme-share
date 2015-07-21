package org.cnv.shr.phone.msg;

import java.io.ByteArrayOutputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.cmn.Services;

public class Dial extends PhoneMessage
{
	private String uniqueKey;
	private PhoneNumberWildCard number;
	
	public Dial(ConnectionParams params)
	{
		super(params);
	}
	public Dial(String key, PhoneNumberWildCard number)
	{
		super(null);
		uniqueKey = key;
		this.number = number;
	}
	
	public PhoneNumberWildCard getNumber()
	{
		return number;
	}
	public String getKey()
	{
		return uniqueKey;
	}
	
	@Override
	public void perform(PhoneLine line, MsgHandler listener) throws Exception
	{
		listener.onDial(line, this);
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("uniqueKey", uniqueKey);
		number.generate(generator, "number");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsNumber = true;
		boolean needsUniqueKey = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsNumber)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs number");
				}
				if (needsUniqueKey)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs uniqueKey");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_OBJECT:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("number")) {
					needsNumber = false;
					number = new org.cnv.shr.phone.cmn.PhoneNumberWildCard(parser);
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			case VALUE_STRING:
				if (key==null) { throw new RuntimeException("Value with no key!"); }
				if (key.equals("uniqueKey")) {
					needsUniqueKey = false;
					uniqueKey = parser.getString();
				} else {
					Services.logger.warning("Unknown key: " + key);
				}
				break;
			default: Services.logger.warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "Dial"; }
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
