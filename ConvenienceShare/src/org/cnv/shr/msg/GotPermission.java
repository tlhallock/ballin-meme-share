package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class GotPermission extends Message
{
	public static final int TYPE = 36;
	
	private String rootName;
	private SharingState permission;

	public GotPermission(String rootName, SharingState permission)
	{
		this.rootName = rootName;
		this.permission = permission;
	}
	
	public GotPermission(InputStream input) throws IOException
	{
		super(input);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		rootName = reader.readString();
		permission = reader.readPermission();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(rootName);
		buffer.append(permission);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine remote = connection.getMachine();
		// Don't change all the other settings...
		remote = DbMachines.getMachine(remote.getIdentifier());
		if (remote == null)
		{
			return;
		}
		if (rootName.length() == 0)
		{
			remote.setTheyShare(permission);
		}
		else
		{
			LocalDirectory directory = DbRoots.getLocalByName(rootName);
			if (directory == null)
			{
				return;
			}
			DbPermissions.setSharingState(remote, directory, permission);
		}
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (rootName!=null)
		generator.write("rootName", rootName);
		if (permission!=null)
		generator.write("permission",permission.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsrootName = true;
		boolean needspermission = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsrootName)
				{
					throw new RuntimeException("Message needs rootName");
				}
				if (needspermission)
				{
					throw new RuntimeException("Message needs permission");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "rootName":
				needsrootName = false;
				rootName = parser.getString();
				break;
			case "permission":
				needspermission = false;
				permission = SharingState.valueOf(parser.getString());;
				break;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "GotPermission"; }
	public GotPermission(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
