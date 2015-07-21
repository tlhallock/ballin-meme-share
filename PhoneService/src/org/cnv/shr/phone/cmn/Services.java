package org.cnv.shr.phone.cmn;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

public class Services
{
	public static final Timer timer = new Timer();
	
	public static final ExecutorService executor = Executors.newCachedThreadPool();
	
	public static final Logger logger = Logger.getLogger(Services.class.getName()); 

	public static MetaHandler handler = new MetaHandler();
	
	
  public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(null);
	public static final JsonParserFactory    parserFactory    = Json.createParserFactory(null);
	public static JsonGenerator createGenerator(OutputStream output, boolean pretty)
	{
		return generatorFactory.createGenerator(output, UTF_8);
	}
	public static JsonParser createParser(InputStream input)
	{
		return parserFactory.createParser(input, UTF_8);
	}
}
