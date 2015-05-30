package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class GotPermission extends Message
{
	public static final int TYPE = 36;
	
	private String rootName;
	private SharingState permission;

	public GotPermission(String rootName, SharingState permission)
	{
		this.rootName = rootName;
		this.permission = permission;
	}
	
	public GotPermission(InputStream input) throws IOException
	{
		super(input);
	}

	@Override
	protected int getType()
	{
		return TYPE;
	}

	@Override
	protected void parse(ByteReader reader) throws IOException
	{
		rootName = reader.readString();
		permission = reader.readPermission();
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(rootName);
		buffer.append(permission);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine remote = connection.getMachine();
		// Don't change all the other settings...
		remote = DbMachines.getMachine(remote.getIdentifier());
		if (remote == null)
		{
			return;
		}
		if (rootName.length() == 0)
		{
			remote.setTheyShare(permission);
		}
		else
		{
			LocalDirectory directory = DbRoots.getLocalByName(rootName);
			if (directory == null)
			{
				return;
			}
			DbPermissions.setSharingState(remote, directory, permission);
		}
	}
}
