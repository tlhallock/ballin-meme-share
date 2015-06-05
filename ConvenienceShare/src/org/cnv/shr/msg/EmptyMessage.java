package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

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

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("size", size);
		generator.writeEnd();
	}

	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("size")) {
				size = new BigDecimal(parser.getString()).intValue();
			}
			break;
			default: break;
			}
		}
	}
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
