package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.msg.key.PermissionFailure;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.OutputByteWriter;

public abstract class Message
{
	private static final int VERSION = 1;
	
	protected Message() {}
	
	// This constructor is no longer needed.
	protected Message(InputStream stream) throws IOException {}
	
	public boolean requiresAthentication()
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Please implement toString() in class " + getClass().getName());
		return builder.toString();
	}
	
	public final void write(OutputByteWriter output) throws IOException
	{
		output.append(getType());
		print(output);
	}
	
	public void read(ByteReader reader) throws IOException
	{
		parse(reader);
	}

	protected abstract int  getType();
	protected abstract void parse(ByteReader reader) throws IOException;
	protected abstract void print(AbstractByteWriter buffer) throws IOException;
	public    abstract void perform(Communication connection) throws Exception;
	
	
	protected void checkPermissionsVisible(Communication c, Machine machine, RootDirectory root, String action) throws PermissionException, IOException
	{
		if (Services.settings.shareWithEveryone.get())
		{
			return;
		}
		SharingState sharing = DbPermissions.isSharing(machine, root);
		if (sharing != null && sharing.canList())
		{
			return;
		}
		
		PermissionFailure permissionFailure = new PermissionFailure(root.getName(), sharing, action);
		c.send(permissionFailure);
		throw new PermissionException(action);
	}
	
	protected void checkPermissionsDownloadable(Communication c, Machine machine, RootDirectory root, String action) throws PermissionException, IOException
	{
		if (Services.settings.shareWithEveryone.get())
		{
			return;
		}
		SharingState sharing = DbPermissions.isSharing(machine, root);
		if (sharing != null && sharing.canDownload())
		{
			return;
		}
		
		PermissionFailure permissionFailure = new PermissionFailure(root.getName(), sharing, action);
		c.send(permissionFailure);
		throw new PermissionException(action);
	}
	
	protected void checkPermissionsViewable(Communication c, Machine machine, String action) throws PermissionException, IOException
	{
		if (Services.settings.shareWithEveryone.get())
		{
			return;
		}
		if (machine.sharingWithOther().canList())
		{
			return;
		}
		
		PermissionFailure permissionFailure = new PermissionFailure(null, machine.sharingWithOther(), action);
		c.send(permissionFailure);
		throw new PermissionException(action);
	}
}
