package org.cnv.shr.dmn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.json.JsonStringSet;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class CompressionList implements Jsonable
{
	private JsonStringSet whiteList = new JsonStringSet();
	private JsonStringSet blackList = new JsonStringSet();
	
	public CompressionList() {}
	
	void reset()
	{
		whiteList.add(".txt"  );
		whiteList.add(".html" );
		whiteList.add(".xml"  );
		whiteList.add(".json" );
		whiteList.add(".java" );
		whiteList.add(".cpp"  );
		whiteList.add(".c"    );
		whiteList.add(".py"   );
		

		blackList.add(".gz"   );
		blackList.add(".rar"  );
		blackList.add(".bz2"  );
		blackList.add(".zip"  );
		blackList.add(".jar"  );
		blackList.add(".war"  );
		blackList.add(".7zip" );
		blackList.add(".7z"   );
		blackList.add(".png"  );
		blackList.add(".jpe"  );
		blackList.add(".jpg"  );
		blackList.add(".avi"  );
		blackList.add(".mp3"  );
		blackList.add(".mp4"  );
		blackList.add(".mkv"  );
	}
	
	private static String getExt(String name)
	{
		int lastIndexOf = name.lastIndexOf('.');
		if (lastIndexOf < 0)
		{
			return null;
		}
		return name.substring(lastIndexOf).toLowerCase();
	}

	public boolean shouldCompressFile(String name)
	{
		String ext = getExt(name);
		if (ext == null)
		{
			return true;
		}
		return !blackList.contains(ext);
	}

	public boolean alwaysCompress(String name)
	{
		String ext = getExt(name);
		if (ext == null)
		{
			return false;
		}
		return whiteList.contains(ext);
	}
	
	public void read(Path p)
	{
		Misc.ensureDirectory(p, true);
		try (JsonParser parser = TrackObjectUtils.createParser(Files.newInputStream(p)))
		{
			parse(parser);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read compression list from file.", e);
			reset();
			write(p);
		}
	}
	
	public void write(Path p)
	{
		Misc.ensureDirectory(p, true);
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(p), true))
		{
			generate(generator, null);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to write compression list from file.", e);
		}
	}
	

	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		{
			generator.writeStartArray("whiteList");
			whiteList.generate(generator);
		}
		{
			generator.writeStartArray("blackList");
			blackList.generate(generator);
		}
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsWhiteList = true;
		boolean needsBlackList = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsWhiteList)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs whiteList");
				}
				if (needsBlackList)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs blackList");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
			case START_ARRAY:
				if (key==null) { LogWrapper.getLogger().warning("Value with no key!"); break; }
				switch(key) {
				case "whiteList":
					needsWhiteList = false;
					whiteList.parse(parser);
					break;
				case "blackList":
					needsBlackList = false;
					blackList.parse(parser);
					break;
				default: LogWrapper.getLogger().warning("Unknown key: " + key);
				}
				break;
			default: LogWrapper.getLogger().warning("Unknown type found in message: " + e);
			}
		}
	}
	public static String getJsonName() { return "CompressionList"; }
	public String getJsonKey() { return getJsonName(); }
	public CompressionList(JsonParser parser) { parse(parser); }
	public String toDebugString() {                                                    
		ByteArrayOutputStream output = new ByteArrayOutputStream();                      
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator, null);                                                     
		}                                                                                
		return new String(output.toByteArray());                                         
	}                                                                                  
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
