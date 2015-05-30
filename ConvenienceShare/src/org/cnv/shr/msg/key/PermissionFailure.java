package org.cnv.shr.msg.key;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.msg.Message;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;

public class PermissionFailure extends Message
{
	public static int TYPE = 35;
	
	private String rootName;
	private SharingState currentPermission;
	private String action;
	
	public PermissionFailure(String root, SharingState current, String action)
	{
		this.rootName = root;
		this.currentPermission = current;
		if (currentPermission == null)
		{
			currentPermission = SharingState.DO_NOT_SHARE;
		}
		this.action = action;
	}
	
	public PermissionFailure(InputStream input) throws IOException
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
		currentPermission = SharingState.get(reader.readInt());
		action = reader.readString();
		
		if (rootName.length() == 0)
		{
			rootName = null;
		}
	}

	@Override
	protected void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		if (rootName == null)
		{
			buffer.append("");
		}
		else
		{
			buffer.append(rootName);
		}
		buffer.append(currentPermission.getDbValue());
		buffer.append(action);
	}

	@Override
	public void perform(Communication connection)
	{
		PermissionFailureEvent event = new PermissionFailureEvent();
		event.machine = connection.getMachine();
		event.rootName = rootName;
		event.currentPermissions = currentPermission;
		event.action = action;
		
		try
		{
			updateDb(event.machine);
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to update permissions for machine " + event.machine, e);
		}
		
		Services.notifications.permissionFailure(event);
		connection.finish();
	}
	
	private void updateDb(Machine machine) throws SQLException
	{
		if (rootName != null)
		{
			RootDirectory root = DbRoots.getRoot(machine, rootName);
			if (root == null)
			{
				return;
			}
			DbPermissions.setSharingState(Services.localMachine, root, currentPermission);
		}
		else
		{
			machine.setTheyShare(currentPermission);
			machine.save();
		}
	}
	
	
	public static class PermissionFailureEvent
	{
		private Machine machine;
		private String rootName;
		private SharingState currentPermissions;
		private String action;
		
		private PermissionFailureEvent() {}
		
		public void show(JFrame frame)
		{
			JOptionPane.showMessageDialog(frame, 
					"Permission was denied while trying action: " + action + ".\n" +
					"Current permissions are " + currentPermissions + ".\n" +
				    "Remote machine: " + machine.getName() + ".\n" +
					(rootName == null ? "" : "Root: " + rootName + ".\n"),
					"Permission failure for " + machine.getName() + ".\n" + 
					"You may be able to hit request permissions...",
					JOptionPane.ERROR_MESSAGE);
		}

		public SharingState getCurrentSharingState()
		{
			return currentPermissions;
		}

		public String getRootName()
		{
			return rootName;
		}
		
		public String getMachineName()
		{
			return machine.getName();
		}

		public String getMachineIdent()
		{
			return machine.getIdentifier();
		}
	}
}
