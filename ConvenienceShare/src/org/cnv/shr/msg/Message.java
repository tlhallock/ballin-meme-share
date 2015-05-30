package org.cnv.shr.msg;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.PermissionFailure;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.OutputByteWriter;

public abstract class Message
{
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
	
	
	protected void checkPermissionsVisible(Communication c, Machine machine, LocalDirectory root, String action) throws PermissionException, IOException
	{
		if (Services.settings.shareWithEveryone.get())
		{
			return;
		}
		SharingState currentPermissions = DbPermissions.getCurrentPermissions(machine, root);
		if (currentPermissions.listable())
		{
			return;
		}
		
		c.send(new PermissionFailure(root.getName(), currentPermissions, action));
		throw new PermissionException(action);
	}
	
	protected void checkPermissionsDownloadable(Communication c, Machine machine, LocalDirectory root, String action) throws PermissionException, IOException
	{
		if (Services.settings.shareWithEveryone.get())
		{
			return;
		}
		SharingState currentPermissions = DbPermissions.getCurrentPermissions(machine, root);
		if (currentPermissions.downloadable())
		{
			return;
		}
		
		c.send(new PermissionFailure(root.getName(), currentPermissions, action));
		throw new PermissionException(action);
	}
	
	protected void checkPermissionsViewable(Communication c, Machine machine, String action) throws PermissionException, IOException
	{
		if (Services.settings.shareWithEveryone.get())
		{
			return;
		}
		SharingState currentPermissions = DbPermissions.getCurrentPermissions(machine);
		if (currentPermissions.listable())
		{
			return;
		}
		
		c.send(new PermissionFailure(null, currentPermissions, action));
		throw new PermissionException(action);
	}
}
