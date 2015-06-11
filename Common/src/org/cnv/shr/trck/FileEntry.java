package org.cnv.shr.trck;

import java.util.Objects;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class FileEntry extends TrackObject
{
	private String checksum;
	private long fileSize;
	
	public FileEntry(String checksum, long fileSize)
	{
		Objects.requireNonNull(checksum);
		this.checksum = checksum;
		this.fileSize = fileSize;
	}

	public FileEntry() {}

//	@Override
//	public void read(JsonParser parser)
//	{
//		String key = null;
//		while (parser.hasNext())
//		{
//			JsonParser.Event e = parser.next();
//			switch (e)
//			{
//			case KEY_NAME:
//				key = parser.getString();
//				break;
//			case VALUE_STRING:
//				String string = parser.getString();
//				if (key == null)
//					break;
//				switch (key)
//				{
//				case "checksum":
//					checksum = string;
//					break;
//				}
//				break;
//			case VALUE_NUMBER:
//				if (key == null)
//					break;
//				BigDecimal bd = new BigDecimal(parser.getString());
//				switch (key)
//				{
//				case "size":
//					fileSize = bd.longValue();
//					break;
//				}
//				bd.intValue();
//				break;
//			case END_OBJECT:
//				return;
//			default:
//			}
//		}
//	}
//
//	@Override
//	public void print(JsonGenerator generator)
//	{
//		generator.writeStartObject();
//		generator.write("checksum", checksum);
//		generator.write("size", fileSize);
//		generator.writeEnd();
//	}

	public void set(String checksum, long fileSize)
	{
		this.checksum = checksum;
		this.fileSize = fileSize;
	}
	
	public String getChecksum()
	{
		return checksum;
	}
	
	public long getFileSize()
	{
		return fileSize;
	}

	@Override
	public int hashCode()
	{
		return (checksum + String.valueOf(fileSize)).hashCode();
	};

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof FileEntry))
		{
			return false;
		}
		FileEntry other = (FileEntry) o;
		return other.getChecksum().equals(getChecksum()) && other.getFileSize() == fileSize;
	}

	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator, String key) {
		if (key!=null)
			generator.writeStartObject(key);
		else
			generator.writeStartObject();
		generator.write("checksum", checksum);
		generator.write("fileSize", fileSize);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsfileSize = true;
		boolean needschecksum = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsfileSize)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs fileSize");
				}
				if (needschecksum)
				{
					throw new org.cnv.shr.util.IncompleteMessageException("Message needs checksum");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("fileSize")) {
				needsfileSize = false;
				fileSize = Long.parseLong(parser.getString());
			}
			break;
		case VALUE_STRING:
			if (key==null) break;
			if (key.equals("checksum")) {
				needschecksum = false;
				checksum = parser.getString();
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "FileEntry"; }
	public String getJsonKey() { return getJsonName(); }
	public FileEntry(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
