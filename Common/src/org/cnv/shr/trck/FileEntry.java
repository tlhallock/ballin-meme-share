package org.cnv.shr.trck;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class FileEntry implements TrackObject
{
	private String checksum;
	private long fileSize;
	
	public FileEntry(String checksum, long fileSize)
	{
		this.checksum = checksum;
		this.fileSize = fileSize;
	}

	public FileEntry() {}

	@Override
	public void read(JsonParser parser)
	{
		String key = null;
		while (parser.hasNext())
		{
			JsonParser.Event e = parser.next();
			switch (e)
			{
			case KEY_NAME:
				key = parser.getString();
				break;
			case VALUE_STRING:
				String string = parser.getString();
				if (key == null)
					break;
				switch (key)
				{
				case "checksum":
					checksum = string;
					break;
				}
				break;
			case VALUE_NUMBER:
				if (key == null)
					break;
				BigDecimal bd = new BigDecimal(parser.getString());
				switch (key)
				{
				case "size":
					fileSize = bd.longValue();
					break;
				}
				bd.intValue();
				break;
			case END_OBJECT:
				return;
			default:
			}
		}
	}

	@Override
	public void print(JsonGenerator generator)
	{
		generator.writeStartObject();
		generator.write("checksum", checksum);
		generator.write("size", fileSize);
		generator.writeEnd();
	}

	public void set(String checksum, long fileSize)
	{
		this.checksum = checksum;
		this.fileSize = fileSize;
	}
	
	public String toString()
	{
		return TrackObjectUtils.toString(this);
	}

	public String getChecksum()
	{
		return checksum;
	}
	
	public long getFileSize()
	{
		return fileSize;
	}
}
