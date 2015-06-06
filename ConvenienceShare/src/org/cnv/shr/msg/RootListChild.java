package org.cnv.shr.msg;

import java.io.IOException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Jsonable;

public class RootListChild implements Jsonable
{
	String name       ;
	String tags       ;
	String description;
	SharingState state;
	
	public RootListChild(ByteReader reader) throws IOException
	{
		parse(reader);
	}
	
	public RootListChild(RootDirectory root)
	{
		name = root.getName();
		tags = root.getTags();
		description = root.getDescription();
		state = DbPermissions.getCurrentPermissions(root.getMachine(), (LocalDirectory) root);
	}
	
	void append(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(name);
		buffer.append(tags);
		buffer.append(description);
		buffer.append(state);
	}
	
	void parse(ByteReader reader) throws IOException
	{
		name        = reader.readString();
		tags        = reader.readString();
		description = reader.readString();
		state = SharingState.get(reader.readInt());
	}
	
	RemoteDirectory getRoot(Machine machine)
	{
		return new RemoteDirectory(machine, name, tags, description, state);
	}

	public String getName()
	{
		return name;
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (name!=null)
		generator.write("name", name);
		if (tags!=null)
		generator.write("tags", tags);
		if (description!=null)
		generator.write("description", description);
		if (state!=null)
		generator.write("state",state.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsname = true;
		boolean needstags = true;
		boolean needsdescription = true;
		boolean needsstate = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsname)
				{
					throw new RuntimeException("Message needs name");
				}
				if (needstags)
				{
					throw new RuntimeException("Message needs tags");
				}
				if (needsdescription)
				{
					throw new RuntimeException("Message needs description");
				}
				if (needsstate)
				{
					throw new RuntimeException("Message needs state");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "name":
				needsname = false;
				name = parser.getString();
				break;
			case "tags":
				needstags = false;
				tags = parser.getString();
				break;
			case "description":
				needsdescription = false;
				description = parser.getString();
				break;
			case "state":
				needsstate = false;
				state = SharingState.valueOf(parser.getString());;
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "RootListChild"; }
	public RootListChild(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
