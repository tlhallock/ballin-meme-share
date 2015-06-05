package org.cnv.shr.dmn.mn;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.msg.DoneMessage;
import org.cnv.shr.msg.DoneResponse;
import org.cnv.shr.msg.EmptyMessage;
import org.cnv.shr.msg.Failure;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.GetPermission;
import org.cnv.shr.msg.GotPermission;
import org.cnv.shr.msg.HeartBeat;
import org.cnv.shr.msg.ListPath;
import org.cnv.shr.msg.ListRoots;
import org.cnv.shr.msg.LookingFor;
import org.cnv.shr.msg.MachineFound;
import org.cnv.shr.msg.PathList;
import org.cnv.shr.msg.RootList;
import org.cnv.shr.msg.ShowApplication;
import org.cnv.shr.msg.UserMessageMessage;
import org.cnv.shr.msg.Wait;
import org.cnv.shr.msg.dwn.ChecksumRequest;
import org.cnv.shr.msg.dwn.ChecksumResponse;
import org.cnv.shr.msg.dwn.ChunkList;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.ChunkResponse;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.DownloadDone;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.msg.dwn.NewAesKey;
import org.cnv.shr.msg.dwn.RequestCompletionStatus;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.ConnectionOpened;
import org.cnv.shr.msg.key.KeyChange;
import org.cnv.shr.msg.key.KeyFailure;
import org.cnv.shr.msg.key.KeyNotFound;
import org.cnv.shr.msg.key.NewKey;
import org.cnv.shr.msg.key.OpenConnection;
import org.cnv.shr.msg.key.PermissionFailure;
import org.cnv.shr.msg.key.RevokeKey;
import org.cnv.shr.msg.key.WhoIAm;
import org.cnv.shr.msg.swup.UpdateInfoMessage;
import org.cnv.shr.msg.swup.UpdateInfoRequest;
import org.cnv.shr.msg.swup.UpdateInfoRequestRequest;
import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.Done;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;

public class GenerateParserCode
{
	private static final String GENERATOR_UNIQUE = "LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK";
	private static final Path[] rootDirs = new Path[]
	{
		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\ConvenienceShare\\src"),
		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Common\\src"),
		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Updater\\src"),
		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Installer\\src"),
		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Tracker\\src"),
	};
	
	
	
	private static final String[] IGNORES = new String[]
	{
		
	};
	
	

	public static void main(String[] args) throws IOException
	{
		for (Class c : new Class[]
			{
				TrackerEntry.class               ,
				MachineEntry.class               ,
				CommentEntry.class               ,
				FileEntry.class                  ,
				Done.class                       ,
				
				
				
				
				
				
				
				
				
				Chunk.class                      ,
				SharedFileId.class               ,
				SharingState.class               ,
				
				
				
				
				
				
				
				ChunkList.class                  ,
				DownloadDone.class               ,
				CompletionStatus.class           ,
				FileRequest.class                ,
				ChunkResponse.class              ,
				RequestCompletionStatus.class    ,
				ChunkRequest.class               ,
				MachineHasFile.class             ,
				ListRoots.class                  ,
				ListPath.class                   ,
				MachineFound.class               ,
				DoneMessage.class                ,
				PathList.class                   ,
				RootList.class                   ,
				FindMachines.class               ,
				UserMessageMessage.class         ,
				Failure.class                    ,
				Wait.class                       ,
				HeartBeat.class                  ,
				LookingFor.class                 ,
				ConnectionOpenAwk.class          ,
				NewKey.class                     ,
				RevokeKey.class                  ,
				ConnectionOpened.class           ,
				KeyNotFound.class                ,
				KeyFailure.class                 ,
				OpenConnection.class             ,
				KeyChange.class                  ,
				WhoIAm.class                     ,
				EmptyMessage.class               ,
				DoneResponse.class               ,
				ChecksumRequest.class            ,
				ChecksumResponse.class           ,
				NewAesKey.class                  ,
				PermissionFailure.class          ,
				GetPermission.class              ,
				GotPermission.class              ,
				UpdateInfoMessage.class          ,
				UpdateInfoRequest.class          ,
				UpdateInfoRequestRequest.class   ,
				ShowApplication.class            ,
			})
		{
			replaceFile(c);
		}
	}

	private static void printParser(PrintStream output, Class c)
	{
		
		if (c.getName().equals("org.cnv.shr.msg.GotPermission"))
		{
			System.out.println("We are here.");
		}
		HashMap<Event, List<Parser>> parsers = new HashMap<>();
		for (Field field : c.getDeclaredFields())
		{
			if (shouldIgnore(field))
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

	private static boolean shouldIgnore(Field field)
	{
		return field.isAnnotationPresent(MyParserIgnore.class) || Modifier.isStatic(field.getModifiers());
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
		ps.println("\tpublic void parse(JsonParser parser) {       ");
		ps.println("\t\tString key = null;                         ");
		ps.println("\t\twhile (parser.hasNext()) {                 ");
		ps.println("\t\t\tJsonParser.Event e = parser.next();      ");
		ps.println("\t\t\tswitch (e)                               ");
		ps.println("\t\t\t{                                        ");
		ps.println("\t\t\tcase END_OBJECT:                         ");
		ps.println("\t\t\t	return;                                ");
		if (parsers.isEmpty())
		{
			ps.println("\t\t\t}                                      ");
			ps.println("\t\t}                                        ");
			ps.println("\t}                                          ");
			return;
		}
		
		ps.println("\t\t\tcase KEY_NAME:                           ");
		ps.println("\t\t\t	key = parser.getString();              ");
		ps.println("\t\t\t	break;                                 ");
		for (Entry<JsonParser.Event, List<Parser>> entry : parsers.entrySet())
		{
			for (Parser p : entry.getValue())
			{
				p.print(ps);
			}
		}
		ps.println("\t\t\tdefault: break;");
		ps.println("\t\t\t}");
		ps.println("\t\t}");
		ps.println("\t}");
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
			// ps.println("\t\t\tkey=null;");
			ps.println("\t\t\tbreak;");
		}
	}
	
	private static String getAssignment(Event jsonType, Field f)
	{
		switch (f.getType().getName())
		{
		case "org.cnv.shr.db.h2.SharingState":
			return "JsonThing.readSharingState(parser)";
		case "org.cnv.shr.dmn.dwn.SharedFileId":
			return "JsonThing.readFileId(parser)";
		case "org.cnv.shr.dmn.dwn.Chunk":
			return "JsonThing.readChunk(parser)";
		case "java.security.PublicKey":
			return "JsonThing.readKey(parser)";
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
//			output.println("\tgenerator.write(\"" + field.getName() + "\", " + field.getName() + ");\n");
		case "org.cnv.shr.dmn.dwn.SharedFileId":
		case "org.cnv.shr.dmn.dwn.Chunk":
			return JsonParser.Event.START_OBJECT;
		case "java.security.PublicKey":
			return JsonParser.Event.VALUE_STRING;
		case "[B":
			break;

		case "boolean":
			return JsonParser.Event.VALUE_TRUE;

		case "org.cnv.shr.db.h2.SharingState":
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
		output.println("\tprotected void generate(JsonGenerator generator) {");
		output.println("\t\tgenerator.writeStartObject();");

		for (Field field : c.getDeclaredFields())
		{
			if (shouldIgnore(field)) continue;
			switch (field.getType().getName())
			{
			case "org.cnv.shr.db.h2.SharingState":
				output.println("\t\tgenerator.write(\"" + field.getName() + "\"," + field.getName() + ".getDbValue());");
				break;
			case "org.cnv.shr.dmn.dwn.SharedFileId":
				break;
			case "org.cnv.shr.dmn.dwn.Chunk":
				break;
			case "java.security.PublicKey":
				output.println("\t\tgenerator.write(\"" + field.getName() + "\", KeyPairObject.serialize(" + field.getName() + "));");
				break;
			case "[B":
				output.println("\t\tgenerator.write(\"" + field.getName() + "\", Misc.format(" + field.getName() + "));");
				break;
				
			case "int": 
			case "java.lang.String":
			case "long":
			case "boolean":
			case "double":
				output.println("\t\tgenerator.write(\"" + field.getName() + "\", " + field.getName() + ");");
				break;
			default:
				System.out.println("\t\t\t" + field.getType() + " " + field.getName());
			}
		}

		output.println("\t\tgenerator.writeEnd();");
		output.println("\t}\n");
	}
	
	private static Path getPathForFile(Class c)
	{
		String other = c.getName().replace(".", "/") + ".java";
		for (Path p : rootDirs)
		{
			Path resolve = p.resolve(other);
			if (Files.exists(resolve))
			{
				return resolve;
			}
		}
		return null;
	}
	
	private static void replaceFile(Class c) throws IOException 
	{
		Path original = getPathForFile(c);
		Path backup   = Paths.get(original.toString() + ".new");
		
		try (PrintStream output = new PrintStream(Files.newOutputStream(backup));
				 BufferedReader input  = Files.newBufferedReader(original);)
		{
			int state = 0;
			
			
			String line;
			while ((line = input.readLine()) != null)
			{
				switch (state)
				{
				case 0: // waiting
					output.println(line);
					if (line.contains(GENERATOR_UNIQUE) && line.contains("BEGIN"))
					{
						state = 1;
					}
					break;
				case 1: // skipping
					if (line.contains(GENERATOR_UNIQUE) && line.contains("END"))
					{
						printGenerator(output, c);
						printParser(output, c);
						output.println(line);
						state = 2;
					}
					break;
				case 2: // finishing
					output.println(line);
					break;
				}
			}
			
			if (state != 2)
			{
				throw new IOException("Did not find GENERATOR_UNIQUE in file " + original);
			}
			
			Files_move(backup, original);
		}
	}
	
	public static void Files_move(Path origin, Path dest) throws IOException
	{
//		Files.move(backup, original, StandardCopyOption.REPLACE_EXISTING);
		try (OutputStream output = Files.newOutputStream(dest);
				 InputStream input  = Files.newInputStream(origin);)
		{
			while (true)
			{
				int read = input.read();
				if (read < 0)
				{
					break;
				}
				output.write(read);
			}
		}
		
		Files.delete(origin);
	}
	
	
	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
