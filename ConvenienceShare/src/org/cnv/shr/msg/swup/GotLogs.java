package org.cnv.shr.msg.swup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.PathSecurity;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.Misc;

public class GotLogs extends Message
{
	private long logSize;
	
	public GotLogs(long logSize2)
	{
		this.logSize = logSize2;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		try
		{
			Machine machine = connection.getMachine();
			Path log =  Paths.get("otherLogs")
					.resolve(PathSecurity.getFsName(machine.getIdentifier()))
					.resolve(String.valueOf(System.currentTimeMillis()));
			
			long remaining = logSize;
			byte[] buffer = new byte[Misc.BUFFER_SIZE];
			Misc.ensureDirectory(log, true);
			try (OutputStream output = Files.newOutputStream(log))
			{
				while (remaining > 0)
				{
					int amountToRead = buffer.length;
					if (amountToRead > remaining)
					{
						amountToRead = (int) remaining;
					}
					int nread = connection.getIn().read(buffer, 0, amountToRead);
					output.write(buffer, 0, nread);
					remaining -= nread;
				}
			}
		}
		finally
		{
			connection.finish();
		}
	}

	@Override
	protected int getType() { return 0; }
	@Override
	protected void parse(ByteReader reader) throws IOException {}
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("logSize", logSize);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needslogSize = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needslogSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs logSize");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("logSize")) {
				needslogSize = false;
				logSize = Long.parseLong(parser.getString());
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "GotLogs"; }
	public String getJsonKey() { return getJsonName(); }
	public GotLogs(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
