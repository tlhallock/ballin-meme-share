
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.dmn.mn;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.cnv.shr.db.h2.MyParserIgnore;
import org.cnv.shr.db.h2.MyParserNullable;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.JsonableUpdateInfo;
import org.cnv.shr.dmn.dwn.Chunk;
import org.cnv.shr.dmn.dwn.SharedFileId;
import org.cnv.shr.dmn.trk.NumFilesMessage;
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
import org.cnv.shr.msg.PathListChild;
import org.cnv.shr.msg.RootList;
import org.cnv.shr.msg.RootListChild;
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
import org.cnv.shr.msg.dwn.DownloadFailure;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.msg.dwn.MachineHasFile;
import org.cnv.shr.msg.dwn.NewAesKey;
import org.cnv.shr.msg.dwn.RequestCompletionStatus;
import org.cnv.shr.msg.key.ConnectionOpenAwk;
import org.cnv.shr.msg.key.ConnectionOpened;
import org.cnv.shr.msg.key.ConnectionReason;
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
import org.cnv.shr.prts.JsonPortMapping;
import org.cnv.shr.prts.PortMapArguments;
import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.Done;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.trck.TrackerRequest;

public class GenerateParserCode
{
	private static final String GENERATOR_UNIQUE = "LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK";
	private static final Path[] rootDirs = new Path[]
	{
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\ConvenienceShare\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Common\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Updater\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Installer\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Tracker\\src"),
		

		Paths.get("/work/ballin-meme-share/ConvenienceShare/src"),
		Paths.get("/work/ballin-meme-share/Common/src"),
		Paths.get("/work/ballin-meme-share/Updater/src"),
		Paths.get("/work/ballin-meme-share/Installer/src"),
		Paths.get("/work/ballin-meme-share/Tracker/src"),
	};
	
	
	private static final HashSet<String> keys = new HashSet<>();
	
	
	public static void main(String[] args) throws IOException
	{
		for (Class<?> c : new Class[]
			{
				TrackerEntry.class               ,
				MachineEntry.class               ,
				CommentEntry.class               ,
				FileEntry.class                  ,
				Done.class                       ,
				
				
				
				PortMapArguments.class           ,
				JsonPortMapping.class            ,
				
				
				
				
				
				
				
				Chunk.class                      ,
				SharedFileId.class               ,
				SharingState.class               ,
				PathListChild.class              ,
				RootListChild.class              ,
				JsonableUpdateInfo.class         ,
				
				TrackerRequest.class             ,
				
				NumFilesMessage.class            ,
				
				
				
				DownloadFailure.class            ,
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
				ConnectionReason.class           ,
				
				org.cnv.shr.db.h2.bak.LocalBackup.class,
				org.cnv.shr.db.h2.bak.MachineBackup.class,
				org.cnv.shr.db.h2.bak.FileBackup.class,
				org.cnv.shr.db.h2.bak.RootPermissionBackup.class,
				org.cnv.shr.db.h2.bak.MessageBackup.class,
				org.cnv.shr.db.h2.bak.DownloadBackup.class,
			})
		{
			try
			{
				replaceFile(c);
			}
			catch (Exception e)
			{
				System.out.println("Error on " + c);
				e.printStackTrace();
				return;
			}
			
			String n = c.getName();
			System.out.println("ALLOCATORS.put(" + n + ".getJsonName(), new JsonAllocator<" + n + ">()          ");
			System.out.println("{                                                                     ");
			System.out.println("\tpublic " + n + " create(JsonParser p) { return new " + n + "(p); }  ");
//			System.out.println("\tString getName() { return \"" + n + "\"; }                          ");
			System.out.println("});                                                                   ");
		}
	}

	private static void printParser(PrintStream output, Class<?> c)
	{
		HashMap<Event, List<Parser>> parsers = getParsersFor(c);
		// if none, we can make it cooler
		printParsers(parsers, output);
	}

	private static HashMap<Event, List<Parser>> getParsersFor(Class<?> c)
	{
		HashMap<Event, List<Parser>> parsers = new HashMap<>();
		while (shouldContinue(c))
		{
			for (Field field : c.getDeclaredFields())
			{
				if (shouldIgnore(field))
				{
					continue;
				}
				List<Parser> parser = getParser(parsers, field);
				if (parser == null)
				{
					System.err.println("Need parser type for " + field.getName());
					continue;
				}
				for (Parser p : parser)
				{
					p.add(field);
				}
			}
			c = c.getSuperclass();
		}
		return parsers;
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
		ps.println("\t@Override                                    ");
		ps.println("\tpublic void parse(JsonParser parser) {       ");
		ps.println("\t\tString key = null;                         ");
                parsers.entrySet().stream().forEach((entry) -> {
                    entry.getValue().get(0).printDecls(ps);
                });
		ps.println("\t\twhile (parser.hasNext()) {                 ");
		ps.println("\t\t\tJsonParser.Event e = parser.next();      ");
		ps.println("\t\t\tswitch (e)                               ");
		ps.println("\t\t\t{                                        ");
		ps.println("\t\t\tcase END_OBJECT:                         ");
//		ps.println("\t\t\t\tvalidate();                            ");
                parsers.entrySet().stream().forEach((entry) -> {
                    entry.getValue().stream().forEach((Parser p) -> {
                        p.printValidator(ps);
                    });
            });
		ps.println("\t\t\t\treturn;                                ");
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
                parsers.entrySet().stream().forEach((entry) -> {
                    entry.getValue().stream().forEach((p) -> {
                        p.print(ps);
                    });
            });
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
		
		public void printDecls(PrintStream ps)
		{
                    fields.stream().filter((f) -> !(f.isAnnotationPresent(MyParserNullable.class))).forEach((f) -> {
                        ps.println("\t\tboolean needs" + f.getName() + " = true;");
                    });
		}
		public void printValidator(PrintStream ps)
		{
			HashSet<String> printed = new HashSet<>();
                        fields.stream().filter((f) -> !(!printed.add(f.getName()) || f.isAnnotationPresent(MyParserNullable.class))).map((f) -> {
                            ps.println("\t\t\t\tif (needs" + f.getName() + ")");
                        return f;
                    }).map((f) -> {
                        ps.println("\t\t\t\t{");
                        ps.println("\t\t\t\t\tthrow new org.cnv.shr.util.IncompleteMessageException(\"Message needs " + f.getName() + "\");");
                        return f;
                    }).forEach((_item) -> {
                        ps.println("\t\t\t\t}");
                    });
		}

		public void print(PrintStream ps)
		{
			ps.println("\t\tcase " + jsonType.name() + ":");
			ps.println("\t\t\tif (key==null) break;");

			if (fields.size() == 1)
			{
                            fields.stream().map((f) -> {
                                ps.println("\t\t\tif (key.equals(\"" + f.getName() + "\")) {");
                                return f;
                            }).map((f) -> {
                                if (!f.isAnnotationPresent(MyParserNullable.class))
                                {
                                    ps.println("\t\t\t\tneeds" + f.getName() + " = false;");
                                }
                                return f;
                            }).forEach((f) -> {
                                ps.println("\t\t\t\t" + f.getName() + getAssignment(jsonType, f) + ";");
                            });
			}
			else
			{
				ps.println("\t\t\tswitch(key) {");
				for (Field f : fields)
				{
					ps.println("\t\t\tcase \"" + f.getName() + "\":");
					if (!f.isAnnotationPresent(MyParserNullable.class))
					{
						ps.println("\t\t\t\tneeds" + f.getName() + " = false;");
					}
					ps.println("\t\t\t\t" + f.getName() + getAssignment(jsonType, f) + ";");
					ps.println("\t\t\t\tbreak;");
				}
			}
			ps.println("\t\t\t}");
			// ps.println("\t\t\tkey=null;");
			ps.println("\t\t\tbreak;");
		}
		
		public void print(StringBuilder builder)
		{
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			try (PrintStream stream = new PrintStream(writer))
			{
				print(stream);
			}
			builder.append(writer.toString());
		}
	}
	
	private static String getAssignment(Event jsonType, Field f)
	{
		switch (f.getType().getName())
		{
		case "org.cnv.shr.json.JsonStringMap":
		case "org.cnv.shr.json.JsonMap":
		case "org.cnv.shr.json.JsonList":
		case "org.cnv.shr.json.JsonStringList": return ".parse(parser)";
		case "class org.cnv.shr.msg.PathList":
			return " = (new PathList(parser)).setParent(this)";
		case "org.cnv.shr.trck.FileEntry":
			return " = new FileEntry(parser)";
		case "org.cnv.shr.db.h2.SharingState":
			return " = SharingState.valueOf(parser.getString())";
		case "org.cnv.shr.dmn.dwn.SharedFileId":
			return " = new SharedFileId(parser)";
		case "org.cnv.shr.dmn.dwn.Chunk":
			return " = new Chunk(parser)";
		case "java.security.PublicKey":
			return " = KeyPairObject.deSerializePublicKey(parser.getString())";

		case "java.lang.Boolean":
		case "boolean":
			switch (jsonType)
			{
			case VALUE_TRUE:  return " = true";
			case VALUE_FALSE: return " = false";
			default:
				return null;
			}
		case "java.lang.Integer":
		case "int":              return " = Integer.parseInt("   + "parser.getString())";
		case "java.lang.Long":  
		case "long":             return " = Long.parseLong("     + "parser.getString())";
		case "java.lang.Double":
		case "double":           return " = Double.parseDouble(" + "parser.getString())";
		
		
		
		
		case "[B":               return " = Misc.format(parser.getString())";
		case "java.lang.String": return " = parser.getString()";
		default:
		}
		return null;
	}

	private static Event getJsonType(String fieldName)
	{
		switch (fieldName)
		{
		case "org.cnv.shr.json.JsonList":
		case "org.cnv.shr.json.JsonStringList":
			return JsonParser.Event.START_ARRAY;
		case "org.cnv.shr.json.JsonStringMap":
		case "org.cnv.shr.json.JsonMap":
			return JsonParser.Event.START_OBJECT;

		case "org.cnv.shr.msg.PathListChild":
		case "org.cnv.shr.msg.RootListChild":
		case "class org.cnv.shr.msg.PathList":
		case "org.cnv.shr.trck.FileEntry":
		case "org.cnv.shr.dmn.dwn.SharedFileId":
		case "org.cnv.shr.dmn.dwn.Chunk":
		case "java.util.HashMap":
			return JsonParser.Event.START_OBJECT;

		case "java.lang.Boolean":
		case "boolean":
			return JsonParser.Event.VALUE_TRUE;

		case "java.lang.Long":
		case "int":
		case "long":
		case "double":
			return JsonParser.Event.VALUE_NUMBER;

		case "org.cnv.shr.db.h2.SharingState":
		case "[B":
		case "java.security.PublicKey":
		case "java.lang.String":
			return JsonParser.Event.VALUE_STRING;
		default:
			System.err.println("Need parser type for " + fieldName);
			return null;
		}
	}

	private static void printGenerator(PrintStream output, Class<?> c)
	{
		output.println("\t@Override");
		output.println("\tpublic void generate(JsonGenerator generator, String key) {");
		output.println("\t\tif (key!=null)");
		output.println("\t\t\tgenerator.writeStartObject(key);");
		output.println("\t\telse");
		output.println("\t\t\tgenerator.writeStartObject();");

		while (shouldContinue(c))
		{
			for (Field field : c.getDeclaredFields())
			{
				if (shouldIgnore(field))
					continue;
				
				printField(output, field.getType(), field.getName(), field.getGenericType(), field.isAnnotationPresent(MyParserNullable.class));
			}
			c = c.getSuperclass();
		}

		output.println("\t\tgenerator.writeEnd();");
		output.println("\t}");
	}

private static void printField(PrintStream output, Class<?> typeName, String fieldName, Type genericType, boolean nullable)
{
	switch (typeName.getName())
	{
	case "java.util.List":
	case "java.util.LinkedList":
	case "java.util.HashMap":
	case "org.cnv.shr.mdl.PathElement":
	case "org.cnv.shr.mdl.RemoteDirectory":
		output.println("fix me");
		break;
	case "org.cnv.shr.json.JsonStringMap":
	case "org.cnv.shr.json.JsonMap":
		if (nullable)
		{
			output.println("\t\tif (" + fieldName + "!=null)");
		}
		output.println("\t\t{");
		output.println("\t\t\tgenerator.writeStartObject(\"" + fieldName + "\");");
		output.println("\t\t\t" + fieldName + ".generate(generator);");
		output.println("\t\t}");
		return;
	case "org.cnv.shr.json.JsonList":
	case "org.cnv.shr.json.JsonStringList":
		if (nullable)
		{
			output.println("\t\tif (" + fieldName + "!=null)");
		}
		output.println("\t\t{");
		output.println("\t\t\tgenerator.writeStartArray(\"" + fieldName + "\");");
		output.println("\t\t\t" + fieldName + ".generate(generator);");
		output.println("\t\t}");
		return;
	case "org.cnv.shr.msg.RootListChild":
	case "org.cnv.shr.dmn.dwn.Chunk":
	case "org.cnv.shr.dmn.dwn.SharedFileId":
	case "org.cnv.shr.trck.FileEntry":
	case "org.cnv.shr.msg.PathListChild":
		if (nullable)
			output.println("\t\tif (" + fieldName + "!=null)");
		output.println("\t\t" + fieldName + ".generate(generator, \"" + fieldName + "\");");
		break;
	case "org.cnv.shr.db.h2.SharingState":
		if (nullable)
			output.println("\t\tif (" + fieldName + "!=null)");
		output.println("\t\tgenerator.write(\"" + fieldName + "\"," + fieldName + ".name());");
		break;
	case "java.security.PublicKey":
		if (nullable)
			output.println("\t\tif (" + fieldName + "!=null)");
		output.println("\t\tgenerator.write(\"" + fieldName + "\", KeyPairObject.serialize(" + fieldName + "));");
		break;
	case "[B":
		if (nullable)
			output.println("\t\tif (" + fieldName + "!=null)");
		output.println("\t\tgenerator.write(\"" + fieldName + "\", Misc.format(" + fieldName + "));");
		break;

	case "java.lang.String":
	case "java.lang.Integer":
	case "java.lang.Long":
	case "java.lang.Boolean":
		if (nullable)
			output.println("\t\tif (" + fieldName + "!=null)");
	case "int":
	case "long":
	case "boolean":
	case "double":
		output.println("\t\tgenerator.write(\"" + fieldName + "\", " + fieldName + ");");
		break;
	default:
		throw new RuntimeException("Nothing for " + typeName + " " + fieldName);
	}
}
	
	private static Path getPathForFile(Class<?> c)
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
	
	private static void replaceFile(Class<?> c) throws IOException 
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
						printType(output, c);
						printConstructor(output, c);
						printToString(output, c);
//						printValidator(output, c);
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
		}
		Files_move(backup, original);
	}

	private static void printToString(PrintStream output, Class<?> c)
	{
		output.println("\tpublic String toDebugString() {                                                    ");
		output.println("\t\tByteArrayOutputStream output = new ByteArrayOutputStream();                      ");
		output.println("\t\ttry (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {");
		output.println("\t\t\tgenerate(generator, null);                                                     ");
		output.println("\t\t}                                                                                ");
		output.println("\t\treturn new String(output.toByteArray());                                         ");
		output.println("\t}                                                                                  ");
	}

	private static void printConstructor(PrintStream ps, Class<?> c)
	{
		if (c.getName().equals(org.cnv.shr.db.h2.SharingState.class.getName()))
		{
			return;
		}
		String substring = c.getName().substring(c.getName().lastIndexOf('.') + 1);
		ps.println("\tpublic " + substring + "(JsonParser parser) { parse(parser); }");
	}

	private static void printType(PrintStream ps, Class<?> c)
	{
		String substring = c.getName().substring(c.getName().lastIndexOf('.') + 1);
		if (!keys.add(substring))
		{
			throw new RuntimeException("Two classes by the same name: " + c.getName());
		}
		ps.println("\tpublic static String getJsonName() { return \"" + substring + "\"; }");
		ps.println("\tpublic String getJsonKey() { return getJsonName(); }");
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
	
	private static boolean shouldContinue(Class<?> c)
	{
		return c != null && c.getName().startsWith("org.cnv.shr");
	}

//private static String parseList(Field f)
//{
//	Type ptype = ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
//	HashMap<Event, List<Parser>> parsersFor = getParsersFor((Class<?>) ptype);
//	
//	Event jsonType = getJsonType(ptype.getTypeName());
//	new Parser(jsonType);
//	
//	StringBuilder builder = new StringBuilder();
//	builder.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
//	builder.append("\t\t\t\twhile (parser.hasNext())                    ").append('\n');
//	builder.append("\t\t\t\t{                                           ").append('\n');
//	builder.append("\t\t\t\t\te = parser.next();                        ").append('\n');
//	builder.append("\t\t\t\t\tswitch (e)                                ").append('\n');
//	builder.append("\t\t\t\t\t{                                         ").append('\n');
//	builder.append("\t\t\t\t\tcase START_OBJECT:                        ").append('\n');
//	
//	for (List<Parser> entry : parsersFor.values())
//	{
//		for (Parser p : entry)
//		{
//			p.print(builder);
//		}
//	}
//	
//	builder.append("\t\t\t\t\tcase END_OBJECT:                          ").append('\n');
////	builder.append("\t\t\t\t\t\t" + f.getName().add())
//	builder.append("\t\t\t\t\tcase END_OBJECT:                          ").append('\n');
//	builder.append("\t\t\t\t\t\t\t\tbreak;                              ").append('\n');
//	
//	builder.append("\t\t\t\t\tcase END_ARRAY:                           ").append('\n');
//	builder.append("\t\t\t\t\t\tbreak;                                  ").append('\n');
//	builder.append("\t\t\t\t\tdefault:                                  ").append('\n');
//	builder.append("\t\t\t\t\t\tbreak;                                  ").append('\n');
//	builder.append("\t\t\t\t\t}                                         ").append('\n');
//	builder.append("\t\t\t\t}                                           ").append('\n');
//	builder.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
//	return builder.toString();
//}
	
//private static void printValidator(PrintStream output, Class c)
//{
//	output.println("\tpublic void validate() {");
//
//	while (shouldContinue(c))
//	{
//		for (Field field : c.getDeclaredFields())
//		{
//			if (shouldIgnore(field))
//				continue;
//			output.println("\t\tjava.util.Objects.requireNonNull(" + field.getName() + ");");
//		}
//		c = c.getSuperclass();
//	}
//	output.println("\t}");
//}
//private static void printFieldArray(PrintStream output, Class typeName, String fieldName, Type genericType)
//{
//	switch (typeName.getName())
//	{
//	case "org.cnv.shr.msg.RootListChild":
//	case "org.cnv.shr.msg.key.MsgMap":
//	case "org.cnv.shr.dmn.dwn.Chunk":
//	case "org.cnv.shr.dmn.dwn.SharedFileId":
//	case "org.cnv.shr.trck.FileEntry":
//	case "org.cnv.shr.msg.PathListChild":
//		output.println("\t\t" + fieldName + ".generate(generator);");
//		break;
//	case "org.cnv.shr.db.h2.SharingState":
//		output.println("\t\tgenerator.write(" + fieldName + ".name());");
//		break;
//	case "java.security.PublicKey":
//		output.println("\t\tgenerator.write(KeyPairObject.serialize(" + fieldName + "));");
//		break;
//	case "[B":
//		output.println("\t\tgenerator.write(Misc.format(" + fieldName + "));");
//		break;
//
//	case "java.lang.Integer":
//	case "java.lang.Long":
//	case "java.lang.Boolean":
//		output.println("\t\tif (" + fieldName + "!=null)");
//	case "int":
//	case "java.lang.String":
//	case "long":
//	case "boolean":
//	case "double":
//		output.println("\t\tgenerator.write(" + fieldName + ");");
//		break;
//	case "java.util.List":
//	case "java.util.LinkedList":
//		ParameterizedType ptype = (ParameterizedType) genericType;
//		Type type = ptype.getActualTypeArguments()[0];
//		
//		output.println("\t\tgenerator.writeStartArray(\"" + fieldName + "\");");
//		output.println("\t\tfor (" + type.getTypeName() + " elem : " + fieldName + ")");
//		output.println("\t\t{");
//		printFieldArray(output, (Class<?>) type, "elem", type);
//		output.println("\t\t}");
//		output.println("\t\tgenerator.writeEnd();");
//		break;
//	default:
//		throw new RuntimeException("Nothing for " + typeName + " " + fieldName);
//	}
//}
//ParameterizedType ptype = (ParameterizedType) genericType;
//Type type = ptype.getActualTypeArguments()[0];
//
//output.println("\t\tgenerator.writeStartArray(\"" + fieldName + "\");");
//output.println("\t\tfor (" + type.getTypeName() + " elem : " + fieldName + ")");
//output.println("\t\t{");
//printFieldArray(output, (Class<?>) type, "elem", type);
//output.println("\t\t}");
//output.println("\t\tgenerator.writeEnd();");
//break;
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
