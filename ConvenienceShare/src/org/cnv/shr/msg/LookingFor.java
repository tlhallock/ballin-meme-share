package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class LookingFor extends Message
{
	public static int TYPE = 28;
	
	private String checksum;
	private long fileSize;
	
	
	public LookingFor(FileEntry file)
	{
		checksum = file.getChecksum();
		fileSize = file.getFileSize();
	}

	public LookingFor(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer)
	{

	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		SharedFile file = DbFiles.getFile(checksum, fileSize);
		Machine machine = connection.getMachine();
		checkPermissionsViewable(connection, machine, "Reporting file");
		connection.send(new MachineHasFile(file));
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (checksum!=null)
		generator.write("checksum", checksum);
		generator.write("fileSize", fileSize);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needschecksum = true;
		boolean needsfileSize = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needschecksum)
				{
					throw new RuntimeException("Message needs checksum");
				}
				if (needsfileSize)
				{
					throw new RuntimeException("Message needs fileSize");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("checksum")) {
				needschecksum = false;
				checksum = parser.getString();
			}
			break;
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("fileSize")) {
				needsfileSize = false;
				fileSize = Long.parseLong(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "LookingFor"; }
	public LookingFor(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
