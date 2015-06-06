package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.JsonThing;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class ChecksumResponse extends Message
{
	public static int TYPE = 33;
	private SharedFileId descriptor;
	private String checksum;

	public ChecksumResponse(SharedFile shared)
	{
		descriptor = new SharedFileId(shared);
		checksum = shared.getChecksum();
	}
	
	public ChecksumResponse(InputStream stream) throws IOException
	{
		super(stream);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Here is your checksum for ").append(descriptor).append(" ").append(checksum);
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		RemoteFile remoteFile = descriptor.getRemote();
		if (DbDownloads.getPendingDownloadId(remoteFile) == null)
		{
			return;
		}
		
		if (remoteFile.getChecksum() == null)
		{
			remoteFile.setChecksum(checksum);
			remoteFile.tryToSave();
		}
		
		UserActions.download(remoteFile);
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		descriptor = reader.readSharedFileId();
		checksum = reader.readString();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(descriptor);
		buffer.append(checksum);
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (descriptor!=null)
		descriptor.generate(generator);
		if (checksum!=null)
		generator.write("checksum", checksum);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needschecksum = true;
		boolean needsdescriptor = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needschecksum)
				{
					throw new RuntimeException("Message needs checksum");
				}
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
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
		case START_OBJECT:
			if (key==null) break;
			if (key.equals("descriptor")) {
				needsdescriptor = false;
				descriptor = JsonThing.readFileId(parser);
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "ChecksumResponse"; }
	public ChecksumResponse(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
