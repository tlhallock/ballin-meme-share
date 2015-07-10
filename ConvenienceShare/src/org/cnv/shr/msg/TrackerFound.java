package org.cnv.shr.msg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class TrackerFound extends Message
{
	private TrackerEntry entry;
	
	public TrackerFound(TrackerEntry entry)
	{
		this.entry = entry;
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine machine = connection.getMachine();
		if (!Services.trackers.shouldAcceptTrackersFrom(machine))
		{
			LogWrapper.getLogger().info("We are not accepting trackers from " + machine + "!");
			connection.finish();
			return;
		}
		Services.trackers.add(entry);
	}


	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		entry.generate(generator, "entry");
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsEntry = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsEntry)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs entry");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_OBJECT:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				if (key.equals("entry")) {
					needsEntry = false;
					entry = new TrackerEntry(parser);
				} else {
					LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "TrackerFound"; }
	public String getJsonKey() { return getJsonName(); }
	public TrackerFound(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	protected int getType() { return 0; }
	@Override
	protected void parse(ByteReader reader) throws IOException {}
	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException {}
}
