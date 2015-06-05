package org.cnv.shr.db.h2;

import java.math.BigDecimal;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public enum SharingState
	{
		DO_NOT_SHARE(1, false, false),
		SHARE_PATHS (2,  true, false),
		DOWNLOADABLE(3,  true,  true),
		;
		@MyParserIgnore
		boolean canList;
		@MyParserIgnore
		boolean canDownload;
		
		int state;
		
		SharingState(int i, boolean cl, boolean cd)
		{
			this.state = i;
			this.canList = cl;
			this.canDownload = cd;
		}
		
		public String humanReadable()
		{
			return name();
		}
		
		public boolean is(int i)
		{
			return state == i;
		}
		
		public static SharingState get(int dbValue)
		{
			for (SharingState s : values())
			{
				if (s.state == dbValue)
				{
					return s;
				}
			}
			return null;
		}

		public int getDbValue()
		{
			return state;
		}

		public boolean downloadable()
		{
			return canDownload;
		}

		public boolean listable()
		{
			return canList;
		}
		
		public boolean isLessOrEquallyRestriveThan(SharingState other)
		{
			return state >= other.state;
//			switch (this)
//			{
//				case DOWNLOADABLE: return true;
//				case SHARE_PATHS:  return other.equals(SHARE_PATHS) || other.equals(DO_NOT_SHARE);
//				case DO_NOT_SHARE: return other.equals(DO_NOT_SHARE);
//				default:           return false;
//			}
		}
		
		public boolean isMoreRestrictiveThan(SharingState other)
		{
			return !isLessOrEquallyRestriveThan(other);
		}
		
		// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	protected void generate(JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("state", state);
		generator.writeEnd();
	}

	public void parse(JsonParser parser) {       
		String key = null;                         
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_NUMBER:
			if (key==null) break;
			if (key.equals("state")) {
				state = new BigDecimal(parser.getString()).intValue();
			}
			break;
			default: break;
			}
		}
	}
		// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	}
