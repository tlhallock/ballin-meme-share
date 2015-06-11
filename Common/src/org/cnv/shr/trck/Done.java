package org.cnv.shr.trck;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class Done extends TrackObject
{
//	@Override
//	public void read(JsonParser parser)
//	{
//		while (parser.hasNext())
//		{
//			JsonParser.Event e = parser.next();
//			switch (e)
//			{
//			case END_OBJECT:
//				return;
//			default:
//				break;
//			}
//		}
//	}
//
//	@Override
//	public void print(JsonGenerator generator)
//	{
//		generator.writeStartObject();
//		generator.writeEnd();
//	}

	
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
	public static String getJsonName() { return "Done"; }
	public String getJsonKey() { return getJsonName(); }
	public Done(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
