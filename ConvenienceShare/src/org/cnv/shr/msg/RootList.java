package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class RootList extends Message
{
	public static int TYPE = 3;
	
	private LinkedList<RootListChild> sharedDirectories = new LinkedList<>();

	public RootList()
	{
		DbIterator<LocalDirectory> listLocals = DbRoots.listLocals();
		while (listLocals.hasNext())
		{
			add(listLocals.next());
		}
	}
	
	public RootList(InputStream i) throws IOException
	{
		super(i);
	}
	
	private void add(RootDirectory root)
	{
		sharedDirectories.add(new RootListChild(root));
	}

	@Override
	public void perform(Communication connection)
	{
		HashSet<String> accountedFor = new HashSet<>();
		Machine machine = connection.getMachine();
		
		boolean changed = true;
		for (RootListChild rootC : sharedDirectories)
		{
			RootDirectory root = rootC.getRoot(connection.getMachine());
			accountedFor.add(root.getName());
			root.setMachine(machine);
			root.tryToSave();
			changed = true;
		}

		List<RootDirectory> toDelete = new LinkedList<>();
		DbIterator<RootDirectory> list = DbRoots.list(machine);
		while (list.hasNext())
		{
			final RootDirectory next = list.next();
			if (accountedFor.contains(next.getName()))
			{
				continue;
			}
			toDelete.add(next);
			DbRoots.deleteRoot(next);
			changed = true;
		}
		
		for (RootDirectory root : toDelete)
		{
			DbRoots.deleteRoot(root);
			// should be deleted...
			Services.notifications.remoteDirectoryChanged((RemoteDirectory) root);
		}
		
		if (changed)
		{
			Services.notifications.remoteChanged(machine);
		}
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		int numFolders = reader.readInt();
		for (int i = 0; i < numFolders; i++)
		{
			sharedDirectories.add(new RootListChild(reader));
		}
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(Services.localMachine.getIdentifier());
		buffer.append(sharedDirectories.size());
		for (RootListChild dir : sharedDirectories)
		{
			dir.append(buffer);
		}
	}
	
	@Override
	protected int getType()
	{
		return TYPE;
	}

	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("Root directories are: ");
		for (RootListChild directory : sharedDirectories)
		{
			builder.append(directory.getName()).append(':');
		}
		
		return builder.toString();
	}

	// GENERATED CODE: DO NET EDIT. BEGIN LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
	@Override
	public void generate(JsonGenerator generator) {
		generator.write(getJsonName());
		generator.writeStartObject();
		generator.writeStartArray("sharedDirectories");
		for (org.cnv.shr.msg.RootListChild elem : sharedDirectories)
		{
		elem.generate(generator);
		}
		generator.writeEnd();
		generator.writeEnd();
	}
	@Override                                    
	public void parse(JsonParser parser) {       
		String key = null;                         
		boolean needssharedDirectories = true;
		while (parser.hasNext()) {                 
			JsonParser.Event e = parser.next();      
			switch (e)                               
			{                                        
			case END_OBJECT:                         
				if (needssharedDirectories)
				{
					throw new RuntimeException("Message needs sharedDirectories");
				}
				return;                                
			case KEY_NAME:                           
				key = parser.getString();              
				break;                                 
		case START_ARRAY:
			if (key==null) break;
			if (key.equals("sharedDirectories")) {
				needssharedDirectories = false;
				sharedDirectories = new LinkedList<>();















				while (parser.hasNext())                    
				{                                           
					e = parser.next();                        
					switch (e)                                
					{                                         
					case START_ARRAY:                         
					case START_OBJECT:                        
					case VALUE_TRUE:                          
					case VALUE_NUMBER:                        
					case VALUE_STRING:                        
						if (key == null)                        
								break;                              
					case END_ARRAY:                           
						break;                                  
					default:                                  
						break;                                  
					}                                         
				}                                           















;
			}
			break;
			default: break;
			}
		}
	}
	public String getJsonName() { return "RootList"; }
	public RootList(JsonParser parser) { parse(parser); }
	// GENERATED CODE: DO NET EDIT. END   LUxNSMW0LBRAvMs5QOeCYdGXnFC1UM9mFwpQtEZyYty536QTKK
}
