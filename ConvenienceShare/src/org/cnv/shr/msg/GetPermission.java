package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class GetPermission extends Message
{
	public static int TYPE = 9;
	
	private String rootName;
	
	public GetPermission(InputStream is) throws IOException
	{
		super(is);
	}
	
	public GetPermission()
	{
		rootName = "";
	}
	
	public GetPermission(RemoteDirectory remote)
	{
		rootName = remote.getName();
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
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(rootName);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine remote = connection.getMachine();
		LocalDirectory local = null;
		
		
		SharingState permission = null;
		if (rootName.length() == 0)
		{
			permission = remote.sharingWithOther();
		}
		else if ((local = DbRoots.getLocalByName(rootName)) == null)
		{
			permission = SharingState.DO_NOT_SHARE;
		}
		else
		{
			permission = DbPermissions.getCurrentPermissions(remote, local);
		}
		
		connection.send(new GotPermission(rootName, permission));
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("rootName", rootName);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsrootName = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsrootName)
				{
					throw new RuntimeException("Message needs rootName");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("rootName")) {
				needsrootName = false;
				rootName = parser.getString();
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "GetPermission"; }
	public GetPermission(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
