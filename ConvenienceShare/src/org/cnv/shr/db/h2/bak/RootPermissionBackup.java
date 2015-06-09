package org.cnv.shr.db.h2.bak;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.Jsonable;

public class RootPermissionBackup implements Jsonable
{
	private String machineIdent;
	private String localName;
	private SharingState currentState;
	
	public RootPermissionBackup(LocalDirectory local, Machine machine, SharingState state)
	{
		this.localName = local.getName();
		this.machineIdent = machine.getIdentifier();
		this.currentState = state;
	}
	
	public void save(ConnectionWrapper wrapper)
	{
		LocalDirectory local = DbRoots.getLocalByName(localName);
		Machine machine = DbMachines.getMachine(machineIdent);
		DbPermissions.setSharingState(machine, local, currentState);
	}
	
	
	// GENERATED CODE: DO NOT EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.writeStartObject();
		if (machineIdent!=null)
		generator.write("machineIdent", machineIdent);
		if (localName!=null)
		generator.write("localName", localName);
		if (currentState!=null)
		generator.write("currentState",currentState.name());
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsmachineIdent = true;
		boolean needslocalName = true;
		boolean needscurrentState = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsmachineIdent)
				{
					throw new RuntimeException("Message needs machineIdent");
				}
				if (needslocalName)
				{
					throw new RuntimeException("Message needs localName");
				}
				if (needscurrentState)
				{
					throw new RuntimeException("Message needs currentState");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case VALUE_STRING:
			if (key==null) break;
			switch(key) {
			case "machineIdent":
				needsmachineIdent = false;
				machineIdent = parser.getString();
				break;
			case "localName":
				needslocalName = false;
				localName = parser.getString();
				break;
			case "currentState":
				needscurrentState = false;
				currentState = SharingState.valueOf(parser.getString());;
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "RootPermissionBackup"; }
	public RootPermissionBackup(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NOT EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
