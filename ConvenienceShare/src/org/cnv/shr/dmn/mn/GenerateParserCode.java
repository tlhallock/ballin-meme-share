package org.cnv.shr.dmn.mn;


import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.msg.MessageReader;

public class GenerateParserCode
{
	
	private static final String[] IGNORES = new String[]
	{
		
	};
	
	

	public static void main(String[] args) throws IOException
	{
		try (PrintStream output = new PrintStream(Files.newOutputStream(Paths.get("output.txt"))))
		{
			MessageReader reader = new MessageReader();
			for (MessageReader.MessageIdentifier ide : reader.getIdentifiers())
			{
				Class c = ide.getConstructor().getDeclaringClass();
				
				output.println(c.getName() + ":");
				printGenerator(output, c);
				printParser(output, c);
			}
		}
	}

	private static void printParser(PrintStream output, Class c)
	{
		HashMap<Event, List<Parser>> parsers = new HashMap<>();
		for (Field field : c.getDeclaredFields())
		{
			if (Modifier.isStatic(field.getModifiers()))
			{
				continue;
			}
			List<Parser> parser = getParser(parsers, field);
			if (parser == null)
			{
				System.out.println("Ignoring " + field.getType().getName());
				continue;
			}
			for (Parser p : parser)
			{
				p.add(field);
			}
		}
		printParsers(parsers, output);
	}
	
	private static List<Parser> getParser(HashMap<Event, List<Parser>> parsers, Field field)
	{
		Event jsonType = getJsonType(field.getType().getName());
		if (jsonType == null)
		{
			return null;
		}
		List<Parser> parser = parsers.get(jsonType);
		if (parser == null)
		{
			parser = new LinkedList<>();
			if (jsonType.equals(JsonParser.Event.VALUE_TRUE))
			{
				parser.add(new Parser(JsonParser.Event.VALUE_FALSE));
				parser.add(new Parser(JsonParser.Event.VALUE_TRUE));
			}
			else
			{
				parser.add(new Parser(jsonType));
			}
			parsers.put(jsonType, parser);
		}
		return parser;
	}
	
	private static void printParsers(HashMap<JsonParser.Event, List<Parser>> parsers, PrintStream ps)
	{
		ps.println("public void parse(JsonParser parser) {         ");
		if (!parsers.isEmpty())
		{
			ps.println("\tString key = null;                         ");
			ps.println("\twhile (parser.hasNext()) {                 ");
			ps.println("\t\tJsonParser.Event e = parser.next();      ");
			ps.println("\t\tswitch (e)                               ");
			ps.println("\t\t{                                        ");
			ps.println("\t\tcase END_OBJECT:                         ");
			ps.println("\t\t	return;                                ");
			ps.println("\t\tcase KEY_NAME:                           ");
			ps.println("\t\t	key = parser.getString();              ");
			ps.println("\t\t	break;                                 ");
			for (Entry<JsonParser.Event, List<Parser>> entry : parsers.entrySet())
			{
				for (Parser p : entry.getValue())
				{
					p.print(ps);
				}
			}
			ps.println("\t\tdefault: break;");
			ps.println("\t\t}");
			ps.println("\t}");
		}
		ps.println("}");
	}
	
	
	static class Parser
	{
		JsonParser.Event jsonType;
		List<Field> fields = new LinkedList<>();
		
		public Parser(Event jsonType2)
		{
			this.jsonType = jsonType2;
		}

		public void add(Field field)
		{
			fields.add(field);
		}

		public void print(PrintStream ps)
		{
			ps.println("\t\tcase " + jsonType.name() + ":");
			ps.println("\t\t\tif (key==null) break;");
			
			if (fields.size() == 1)
			{
			for (Field f : fields)
			{
				ps.println("\t\t\tif (key.equals(\"" + f.getName() + "\")) {");
				ps.println("\t\t\t\t" + f.getName() + " = " + getAssignment(jsonType, f) + ";");
			}
			}
			else
			{
				ps.println("\t\t\tswitch(key) {");
			for (Field f : fields)
			{
				ps.println("\t\t\tcase \"" + f.getName() + "\":");
				ps.println("\t\t\t\t" + f.getName() + " = " + getAssignment(jsonType, f) + ";");
				ps.println("\t\t\t\tbreak;");
			}
			}
			ps.println("\t\t\t}");
//			ps.println("\t\t\tkey=null;");
			ps.println("\t\t\tbreak;");
		}
	}
	
	private static String getAssignment(Event jsonType, Field f)
	{
		switch (f.getType().getName())
		{
		case "org.cnv.shr.db.h2.DbPermissions$SharingState":
			return "readSharingState(parser)";
		case "org.cnv.shr.dmn.dwn.SharedFileId":
			return "readFileId(parser)";
		case "org.cnv.shr.dmn.dwn.Chunk":
			return "readChunk(parser)";
		case "java.security.PublicKey":
			return "readKey(parser)";
		case "[B":
			break;

		case "boolean":
			switch (jsonType)
			{
			case VALUE_TRUE:  return "true";
			case VALUE_FALSE: return "false";
			}
		case "int": return "new BigDecimal(parser.getString()).intValue()";
		case "long":return "new BigDecimal(parser.getString()).longValue()";
		case "double": return "new BigDecimal(parser.getString()).doubleValue()";
		case "java.lang.String": return "parser.getString()";
		default:
		}
		return null;
	}

	private static Event getJsonType(String fieldName)
	{
		switch (fieldName)
		{
		case "org.cnv.shr.db.h2.DbPermissions$SharingState":
//			output.println("\tgenerator.write(\"" + field.getName() + "\", " + field.getName() + ");\n");
		case "org.cnv.shr.dmn.dwn.SharedFileId":
		case "org.cnv.shr.dmn.dwn.Chunk":
		case "java.security.PublicKey":
			return JsonParser.Event.START_OBJECT;
		case "[B":
			break;

		case "boolean":
			return JsonParser.Event.VALUE_TRUE;
			
		case "int":
		case "long":
		case "double":
			return JsonParser.Event.VALUE_NUMBER;
			
			
		case "java.lang.String":
			return JsonParser.Event.VALUE_STRING;
		default:
		}
		return null;
	}

	private static void printGenerator(PrintStream output, Class c)
	{
		output.println("protected void generate(JsonGenerator g) {\n");

		for (Field field : c.getDeclaredFields())
		{
			switch (field.getType().getName())
			{
			case "org.cnv.shr.db.h2.DbPermissions$SharingState":
//						output.println("\tgenerator.write(\"" + field.getName() + "\", " + field.getName() + ");\n");
				break;
			case "org.cnv.shr.dmn.dwn.SharedFileId":
				break;
			case "org.cnv.shr.dmn.dwn.Chunk":
				break;
			case "java.security.PublicKey":
				break;
			case "[B":
				break;
				
				
			case "int": if (field.getName().equals("TYPE")) break;
			case "java.lang.String":
			case "long":
			case "boolean":
			case "double":
				output.println("\tgenerator.write(\"" + field.getName() + "\", " + field.getName() + ");");
				break;
			default:
				System.out.println("\t\t" + field.getType() + " " + field.getName());
			}
		}

		output.println("}\n");
	}
	
	private static void replaceFile(Path f, Class c) 
	{
		
	}
}
