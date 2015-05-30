package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public class GetPermission extends Message
{
	public static int TYPE = 35;
	
	private String rootName;
	
	public GetPermission(InputStream is) throws IOException
	{
		super(is);
	}
	
	public GetPermission()
	{
		rootName = "";
	}
	
	public GetPermission(RemoteDirectory remote)
	{
		rootName = remote.getName();
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
	}

	@Override
	protected void print(AbstractByteWriter buffer) throws IOException
	{
		buffer.append(rootName);
	}

	@Override
	public void perform(Communication connection) throws Exception
	{
		Machine remote = connection.getMachine();
		LocalDirectory local = null;
		
		
		SharingState permission = null;
		if (rootName.length() == 0)
		{
			permission = remote.sharingWithOther();
		}
		else if ((local = DbRoots.getLocalByName(rootName)) == null)
		{
			permission = SharingState.DO_NOT_SHARE;
		}
		else
		{
			permission = DbPermissions.getCurrentPermissions(remote, local);
		}
		
		connection.send(new GotPermission(rootName, permission));
	}
}
