package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class EmptyMessage extends Message
{
	private int size;
	
	public EmptyMessage(int size)
	{
		this.size = size;
	}
	
	public EmptyMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	public static int TYPE = 30;
	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		size = reader.readInt();
		for (int i = 0; i < size; i++)
		{
			reader.readByte();
		}
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(size);
		for (int i = 0; i < size; i++)
		{
			buffer.append((byte) 0);
		}
	}

	@Override
	public void perform(Communication connection) {}
	
	public String toString()
	{
		return "Filler of size " + size;
	}

	public boolean requiresAthentication()
	{
		return false;
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("size", size);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needssize = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needssize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs size");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("size")) {
				needssize = false;
				size = Integer.parseInt(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "EmptyMessage"; }
	public String getJsonKey() { return getJsonName(); }
	public EmptyMessage(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
