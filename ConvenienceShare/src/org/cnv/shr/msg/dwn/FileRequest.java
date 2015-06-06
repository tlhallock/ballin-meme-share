package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class FileRequest extends DownloadMessage
{
	private int chunkSize;

	public static int TYPE = 13;

	public FileRequest(RemoteFile remoteFile, int chunkSize)
	{
		super(remoteFile.getFileEntry());
		this.chunkSize = chunkSize;
	}
	
	public FileRequest(InputStream stream) throws IOException
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
		
		builder.append("I wanna download " + getDescriptor() + " in chunksizes " + chunkSize);
		
		return builder.toString();
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		LocalFile local = getLocal();
		checkPermissionsDownloadable(connection, connection.getMachine(), local.getRootDirectory(), "Serve file");
		ServeInstance serve = Services.server.serve(local, connection);
		serve.sendChunks(chunkSize);
	}

	@Override
	protected void finishParsing(ByteReader reader) throws IOException
	{
		chunkSize = reader.readInt();
		
	}

	@Override
	protected void finishWriting(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(chunkSize);
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.write("chunkSize", chunkSize);
		if (descriptor!=null)
		descriptor.generate(generator);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsdescriptor = true;
		boolean needschunkSize = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsdescriptor)
				{
					throw new RuntimeException("Message needs descriptor");
				}
				if (needschunkSize)
				{
					throw new RuntimeException("Message needs chunkSize");
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
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("chunkSize")) {
				needschunkSize = false;
				chunkSize = Integer.parseInt(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "FileRequest"; }
	public FileRequest(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
