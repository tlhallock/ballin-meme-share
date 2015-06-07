package org.cnv.shr.dmn.dwn;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.Jsonable;

public class SharedFileId implements Jsonable
{
	// Either
	private String machineIdent;
	private String rootName;
	private String path;
	
	public SharedFileId(SharedFile file)
	{
		this.machineIdent = file.getRootDirectory().getMachine().getIdentifier();
		this.rootName = file.getRootDirectory().getName();
		this.path = file.getPath().getFullPath();
	}

	public SharedFileId(String readString, String readString2, String readString3)
	{
		this.machineIdent = readString;
		this.rootName = readString2;
		this.path = readString3;
	}

	public String getMachineIdent()
	{
		return machineIdent;
	}

	public String getRootName()
	{
		return rootName;
	}

	public String getPath()
	{
		return path;
	}

	@Override
	public int hashCode()
	{
		return (machineIdent + ":" + rootName + ":" + path).hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof SharedFileId))
		{
			return false;
		}
		SharedFileId o = (SharedFileId) other;
		return machineIdent.equals(o.machineIdent)
				&& rootName.equals(o.rootName)
				&& path.equals(path);
	}
	
	public LocalFile getLocal()
	{
		PathElement pathElement = DbPaths.getPathElement(path);
		LocalDirectory local = DbRoots.getLocalByName(rootName);
		return DbFiles.getFile(local, pathElement);
	}
	
	public RemoteFile getRemote()
	{
		Machine machine = DbMachines.getMachine(machineIdent);
		RootDirectory root = DbRoots.getRoot(machine, path);
		PathElement pathElement = DbPaths.getPathElement(path);
		return (RemoteFile) DbFiles.getFile(root, pathElement);
	}
	
	@Override
	public String toString()
	{
		return machineIdent + " : " + rootName + " : " + path;
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		if (machineIdent!=null)
		generator.write("machineIdent", machineIdent);
		if (rootName!=null)
		generator.write("rootName", rootName);
		if (path!=null)
		generator.write("path", path);
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needsmachineIdent = true;
		boolean needsrootName = true;
		boolean needspath = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needsmachineIdent)
				{
					throw new RuntimeException("Message needs machineIdent");
				}
				if (needsrootName)
				{
					throw new RuntimeException("Message needs rootName");
				}
				if (needspath)
				{
					throw new RuntimeException("Message needs path");
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
			case "rootName":
				needsrootName = false;
				rootName = parser.getString();
				break;
			case "path":
				needspath = false;
				path = parser.getString();
				break;
			}
			break;
			default: break;
			}
		}
	}
	public static String getJsonName() { return "SharedFileId"; }
	public SharedFileId(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
