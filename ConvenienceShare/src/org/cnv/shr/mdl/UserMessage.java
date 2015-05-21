package org.cnv.shr.mdl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.swing.JOptionPane;

import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbPermissions.SharingState;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.dmn.Services;

public class UserMessage extends DbObject<Integer>
{
	public static final int MAX_MESSAGE_LENGTH = 1024;
	
	private Machine machine;
	private MessageType type;
	// actually received...
	private long sent;
	private String message;
	
	public UserMessage(Integer int1)
	{
		super(int1);
	}
	
	public UserMessage(Machine machine, int type, String message)
	{
		super(null);
		this.machine = machine;
		this.sent = System.currentTimeMillis();
		this.type = MessageType.getMessageType(type);
		this.message = message;
	}
	
	private UserMessage(MessageType type, String message)
	{
		super(null);
		this.type = type;
		this.message = message;
		this.sent = System.currentTimeMillis();
		this.machine = Services.localMachine;
	}

	@Override
	public void fill(Connection c, ResultSet row, DbLocals locals) throws SQLException
	{
		int ndx = 1;
		this.id = row.getInt(ndx++);
		this.machine = (Machine) locals.getObject(c, DbObjects.RMACHINE, row.getInt(ndx++));
		sent = row.getLong(ndx++);
		type = MessageType.getMessageType(row.getInt(ndx++));
		message = row.getString(ndx++);
	}

	@Override
	public boolean save(Connection c) throws SQLException
	{
		if (machine == null)
			return false;
		
		try (PreparedStatement stmt = c.prepareStatement(
				"insert into MESSAGE values (DEFAULT, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS);)
		{
			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());
			stmt.setLong(ndx++, sent);
			stmt.setInt(ndx++, type.dbValue);
			stmt.setString(ndx++, message);
			stmt.executeUpdate();
			ResultSet generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next())
			{
				id = generatedKeys.getInt(1);
				return true;
			}
			return false;
		}
	}

	public void open()
	{
		if (type == null)
		{
			Services.logger.println("Bad message type: null");
			return;
		}
		switch (type)
		{
		case SHARE:
			shareMachine();
			break;
		case SHARE_ROOT:
			shareRoot(SharingState.DOWNLOADABLE);
			break;
		case SEE_ROOT:
			shareRoot(SharingState.SHARE_PATHS);
			break;
		case TEXT:
			showMessage();
			break;
		default:
			Services.logger.println("Unknown message type: " + type.dbValue);
		}
	}
	
	private void shareMachine()
	{
		if (machine == null)
		{
			Services.logger.println("No machine.");
			return;
		}
		
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
			    Services.application,
			    "Would you like to share with machine " + machine.getName(),
			    "Share message sent on " + new Date(sent),
			    JOptionPane.YES_NO_OPTION))
		{
			machine.setSharing(SharingState.DOWNLOADABLE);
			try
			{
				machine.save();
				Services.notifications.remoteChanged(machine);
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void shareRoot(SharingState state)
	{
		if (machine == null)
		{
			Services.logger.println("No machine.");
			return;
		}
		LocalDirectory localByName = DbRoots.getLocalByName(message);
		if (localByName == null)
		{
			Services.logger.println("Bad message: unknown root.");
			return;
		}
		
		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
			    Services.application,
			    "Shoule you like to share root \"" + localByName.getName() + "\" with machine \"" + machine.getName() + "\"",
			    "Share root message sent on " + new Date(sent),
			    JOptionPane.YES_NO_OPTION))
		{
			DbPermissions.share(machine, localByName, state);
			Services.notifications.remoteChanged(machine);
		}
	}
	
	public boolean checkInsane()
	{
		if (type == null)
		{
			Services.logger.println("No type.");
			return true;
		}
		if (machine == null)
		{
			Services.logger.println("No machine.");
			return true;
		}
		
		LocalDirectory localByName;
		switch (type)
		{
		case SHARE:
			if (machine.isSharing().canDownload())
			{
				Services.logger.println("Already sharing.");
				return true;
			}
			break;
		case SHARE_ROOT:
			localByName = DbRoots.getLocalByName(message);
			if (localByName == null)
			{
				Services.logger.println("No local");
				return true;
			}
			if (DbPermissions.isSharing(machine, localByName).canDownload())
			{
				Services.logger.println("Already sharing");
				return true;
			}
			break;
		case SEE_ROOT:
			localByName = DbRoots.getLocalByName(message);
			if (localByName == null)
			{
				Services.logger.println("No local");
				return true;
			}
			if (DbPermissions.isSharing(machine, localByName).canList())
			{
				Services.logger.println("Already visible");
				return true;
			}
		case TEXT: return false;
		default: 
			Services.logger.println("Unkown request type.");
			return true;
		}
		return false;
	}

	private void showMessage()
	{
		if (machine == null)
		{
			Services.logger.println("No machine.");
			return;
		}
		JOptionPane.showMessageDialog(Services.application, 
				"Machine " + machine.getName() + " says \"" + message + "\"",
				"Message sent on " + new Date(sent),
				JOptionPane.INFORMATION_MESSAGE);
	}

	public static UserMessage createShareRequest()
	{
		return new UserMessage(MessageType.SHARE, "");
	}
	public static UserMessage createShareRootRequest(RootDirectory remote)
	{
		return new UserMessage(MessageType.SHARE_ROOT, remote.getName());
	}
	public static UserMessage createListRequest(RootDirectory remote)
	{
		return new UserMessage(MessageType.SEE_ROOT, remote.getName());
	}
	public static UserMessage createTextMessage(String text)
	{
		return new UserMessage(MessageType.TEXT, text);
	}

    public int getType() {
        return type.dbValue;
    }

    public String getMessage() {
        return message;
    }

	public long getSent()
	{
		return sent;
	}
	public MessageType getMessageType()
	{
		return type;
	}

	public Machine getMachine()
	{
		return machine;
	}
	
	public static enum MessageType
	{
		SHARE(0),
		SHARE_ROOT(1),
		SEE_ROOT(2),
		TEXT(3)
		
		;
		
		int dbValue;
		MessageType(int v)
		{
			this.dbValue = v;
		}
		
		static MessageType getMessageType(int v)
		{
			for (MessageType t : values())
			{
				if (t.dbValue == v)
				{
					return t;
				}
			}
			return null;
		}

		public String humanReadable()
		{
			return name();
		}

		public int getDbValue()
		{
			return dbValue;
		}
	}
}
