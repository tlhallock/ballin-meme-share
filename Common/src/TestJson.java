import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.stream.JsonParser;


public class TestJson
{
	public static void main(String[] args)
	{
		String json = 
				   "{                                                                  \n" +
            "    \"tags\":\"\",                                                \n" + 
            "    \"lastModified\":1435460634000                                \n" + 
            "}                                                                 \n";

		try (JsonParser parser = Json.createParser(new InputStreamReader(new ByteArrayInputStream(json.getBytes()), Charset.forName("UTF-8"))
		{
			@Override
			public synchronized int read(char[] arr, int off, int len) throws IOException
			{
				return super.read(arr, off, 1);
			}
		});)
		{
			while (parser.hasNext())
			{
				switch (parser.next())
				{
				case KEY_NAME:
					System.err.println("Key: \"" + parser.getString() + "\"");
					break;
				case VALUE_STRING:
					System.err.println("String: \"" + parser.getString() + "\"");
					break;
				case VALUE_NUMBER:
					System.err.println("Number: \"" + parser.getBigDecimal() + "\"");
					break;
				}
			}
		}
	}
}
