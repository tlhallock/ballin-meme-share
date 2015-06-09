package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class MachineHasFile extends DownloadMessage
{
	private boolean hasFile;
	
	public static int TYPE = 17;
	
	public MachineHasFile(SharedFile file)
	{
		super(file.getFileEntry());
		
		if (file == null)
		{
			hasFile = false;
		}
		else
		{
			hasFile = true;
		}
	}

	public MachineHasFile(InputStream stream) throws IOException
	{
		super(stream);
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		hasFile = reader.readBoolean();
	}
	
	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(hasFile);
	}
	
	@Override
	public void perform(Communication connection) throws Exception
	{
		// Maybe I should not have sent it?
		// Maybe this should be to remove the connection already present.
		// No.
		if (!hasFile) return;
		

		DownloadInstance downloadInstance = Services.downloads.getDownloadInstance(getDescriptor(), connection);
		if (downloadInstance == null)
		{
			return;
		}
		downloadInstance.addSeeder(connection.getMachine(), connection);
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("I got it! ").append(hasFile).append(":").append(getDescriptor());
		
		return builder.toString();
	}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("hasFile", hasFile);
		if (descriptor!=null)
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdescriptor = true;
		boolean needshasFile = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
				}
				if (needshasFile)
				{
					throw new RuntimeException("Message needs hasFile");
				}
				if (needshasFile)
				{
					throw new RuntimeException("Message needs hasFile");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("descriptor")) {
				needsdescriptor = false;
				descriptor = new FileEntry(parser);
			}
			break;
		case VALUE_FALSE:
			if (key==null) break;
			if (key.equals("hasFile")) {
				needshasFile = false;
				hasFile = false;
			}
			break;
		case VALUE_TRUE:
			if (key==null) break;
			if (key.equals("hasFile")) {
				needshasFile = false;
				hasFile = true;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "MachineHasFile"; }
	public MachineHasFile(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
