package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class FindMachines extends Message
{
	public static int TYPE = 4;
	
	public FindMachines() {}
	
	public FindMachines(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void parse(ByteReader reader) throws IOException {}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) {}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		connection.send(new MachineFound());
		DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();
		while (listRemoteMachines.hasNext())
		{
			MachineFound m = new MachineFound(listRemoteMachines.next());
			if (m.equals(connection.getMachine()))
			{
				continue;
			}
			
			connection.send(m);
		}
	}
	
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Find machines");
		
		return builder.toString();
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
	public static String getJsonName() { return "FindMachines"; }
	public String getJsonKey() { return getJsonName(); }
	public FindMachines(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
