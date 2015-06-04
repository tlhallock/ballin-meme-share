//package org.cnv.shr.msg;
//
//import java.lang.reflect.Field;
//import java.math.BigDecimal;
//import java.util.HashMap;
//
//import javax.json.JsonArray;
//import javax.json.JsonException;
//import javax.json.JsonObject;
//import javax.json.stream.JsonParser;
//
//public class MyParser
//{
//
//	private Message m;
//	private HashMap<String, Field> fields;
//
//	MyParser(Message m)
//	{
//		this.m = m;
//		for (Field file : m.getClass().getFields())
//		{
//			fields.put(file.getName(), file);
//		}
//	}
//
//	private void read(JsonParser parser)
//	{
//		Field field = null;
//		while (parser.hasNext())
//		{
//			JsonParser.Event e = parser.next();
//			switch (e)
//			{
//			case KEY_NAME:
//				field = fields.get(parser.getString());
//				break;
//			case START_ARRAY:
//				JsonArray array = readArray(new JsonArrayBuilderImpl());
//				builder.add(key, array);
//				break;
//			case START_OBJECT:
//				JsonObject object = readObject(new JsonObjectBuilderImpl());
//				builder.add(key, object);
//				break;
//			case VALUE_STRING:
//				String string = parser.getString();
//				builder.add(key, string);
//				break;
//			case VALUE_NUMBER:
//				BigDecimal bd = new BigDecimal(parser.getString());
//				builder.add(key, bd);
//				break;
//			case VALUE_TRUE:
//				builder.add(key, true);
//				break;
//			case VALUE_FALSE:
//				builder.add(key, false);
//				break;
//			case VALUE_NULL:
//				builder.addNull(key);
//				break;
//			case END_OBJECT:
//				return builder.build();
//			default:
//				throw new JsonException("Internal Error");
//			}
//		}
//		throw new JsonException("Internal Error");
//	}
//}
