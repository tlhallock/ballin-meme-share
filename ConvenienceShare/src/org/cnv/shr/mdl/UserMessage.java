
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.mdl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.DbTables.DbObjects;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.AcceptKey;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class UserMessage extends DbObject<Integer>
{
	private static final QueryWrapper INSERT1 = new QueryWrapper("insert into MESSAGE values (DEFAULT, ?, ?, ?, ?)");

	public static final int MAX_MESSAGE_LENGTH = 8192;
	
	private Machine machine;
	private MessageType type;
	// actually received...
	private long sent;
	protected String message;
	
	public UserMessage(Integer int1)
	{
		super(int1);
	}
	
	public UserMessage(Machine machine, int type, String message)
	{
		this(machine, type, message, System.currentTimeMillis());
	}
	
	private UserMessage(MessageType type, String message)
	{
		this (Services.localMachine, type.dbValue, message);
	}

	public UserMessage(Machine machine, int type, String message, long added)
	{
		this(null);
		this.machine = machine;
		this.sent = added;
		this.type = MessageType.getMessageType(type);
		this.message = message;
	}

	@Override
	public void fill(ConnectionWrapper c, ResultSet row, DbLocals locals) throws SQLException
	{
		int ndx = 1;
		this.id = row.getInt(ndx++);
		this.machine = (Machine) locals.getObject(c, DbObjects.MACHINE, row.getInt(ndx++));
		sent = row.getLong(ndx++);
		type = MessageType.getMessageType(row.getInt(ndx++));
		message = row.getString(ndx++);
	}

	@Override
	public boolean save(ConnectionWrapper c) throws SQLException
	{
		if (machine == null)
			return false;
		
		if (message != null && message.length() > UserMessage.MAX_MESSAGE_LENGTH)
		{
			message = message.substring(0, UserMessage.MAX_MESSAGE_LENGTH);
		}

		try (StatementWrapper stmt = c.prepareStatement(INSERT1, Statement.RETURN_GENERATED_KEYS);)
		{
			int ndx = 1;
			stmt.setInt(ndx++, machine.getId());
			stmt.setLong(ndx++, sent);
			stmt.setInt(ndx++, type.dbValue);
			stmt.setString(ndx++, message);
			stmt.executeUpdate();
			try (ResultSet generatedKeys = stmt.getGeneratedKeys();)
			{
				if (generatedKeys.next())
				{
					id = generatedKeys.getInt(1);
					return true;
				}
				return false;
			}
		}
	}

	public void open(JFrame origin)
	{
		if (type == null)
		{
			LogWrapper.getLogger().info("Bad message type: null");
			return;
		}
		switch (type)
		{
		case SHARE:
			shareMachine(origin);
			break;
		case SHARE_ROOT:
			shareRoot(origin, SharingState.DOWNLOADABLE);
			break;
		case SEE_ROOT:
			shareRoot(origin, SharingState.SHARE_PATHS);
			break;
		case TEXT:
			showMessage(origin);
			break;
		case CHANGE_IDENT:
			showChangeIdent(origin);
			break;
		case CHANGE_KEYS:
			showChangeKey(origin);
			break;
		default:
			LogWrapper.getLogger().info("Unknown message type: " + type.dbValue);
		}
	}
	
	private void showChangeKey(JFrame origin)
	{
		ChangeKeyUserMessageInfo info = new ChangeKeyUserMessageInfo(TrackObjectUtils.createParser(message));
		AcceptKey.showAcceptDialog(info.url, info.machineName, info.machineIdentifier, KeyPairObject.deSerializePublicKey(info.publicKey));
	}

	private void showChangeIdent(JFrame origin)
	{
		ChangeIdentifierUserMessage info = new ChangeIdentifierUserMessage(TrackObjectUtils.createParser(message));
		
		if (machine != null && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
				Services.notifications.getCurrentContext(),
				"For machine at " + info.ip + ":" + info.port + " we expected to find an identifier of\n"
					+ "\"" + machine.getIdentifier() + "\"\nbut instead it was\n\"" + info.newIdentifer + "\n"
					+ "Would you like to remove the previous machine and add a new one?\n This will change keys and remove all cached data.\n",
				"Found wrong machine at " + info.newIdentifer,
				JOptionPane.YES_NO_OPTION))
		{
			return;
		}
		DbMachines.delete(machine);
		info.add();
	}

	private void shareMachine(JFrame origin)
	{
		if (machine == null)
		{
			LogWrapper.getLogger().info("No machine.");
			return;
		}

		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				origin, 
				"Would you like to share with machine " + machine.getName(), 
				"Share message sent on " + new Date(sent), 
				JOptionPane.YES_NO_OPTION))
		{
			machine.setWeShare(SharingState.DOWNLOADABLE);
			machine.tryToSave();
			Services.notifications.remoteChanged(machine);
		}
	}

	private void shareRoot(JFrame origin, SharingState state)
	{
		if (machine == null)
		{
			LogWrapper.getLogger().info("No machine.");
			return;
		}
		LocalDirectory localByName = DbRoots.getLocalByName(message);
		if (localByName == null)
		{
			LogWrapper.getLogger().info("Bad message: unknown root.");
			return;
		}

		if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				origin,
				"Would you like to share root \"" + localByName.getName() + "\" with machine \"" + machine.getName() + "\"",
				"Share root message sent on " + new Date(sent),
				JOptionPane.YES_NO_OPTION))
		{
			if (localByName.getDefaultSharingState().isMoreRestrictiveThan(state))
			{
				localByName.setDefaultSharingState(state);
				localByName.tryToSave();
			}
			DbPermissions.setSharingState(machine, localByName, state);
			Services.notifications.remoteChanged(machine);
		}
	}
	
	public boolean checkInsane()
	{
		if (type == null)
		{
			LogWrapper.getLogger().info("No type.");
			return true;
		}
		if (machine == null)
		{
			LogWrapper.getLogger().info("No machine.");
			return true;
		}
		
		LocalDirectory localByName;
		switch (type)
		{
		case SHARE:
			if (DbPermissions.getCurrentPermissions(machine).downloadable())
			{
				LogWrapper.getLogger().info("Already sharing.");
				return true;
			}
			break;
		case SHARE_ROOT:
			localByName = DbRoots.getLocalByName(message);
			if (localByName == null)
			{
				LogWrapper.getLogger().info("No local");
				return true;
			}
			if (DbPermissions.getCurrentPermissions(machine, localByName).downloadable())
			{
				LogWrapper.getLogger().info("Already sharing");
				return true;
			}
			break;
		case SEE_ROOT:
			localByName = DbRoots.getLocalByName(message);
			if (localByName == null)
			{
				LogWrapper.getLogger().info("No local");
				return true;
			}
			if (DbPermissions.getCurrentPermissions(machine, localByName).listable())
			{
				LogWrapper.getLogger().info("Already visible");
				return true;
			}
		case TEXT: return false;
		default: 
			LogWrapper.getLogger().info("Unkown request type.");
			return true;
		}
		return false;
	}

	private void showMessage(JFrame origin)
	{
		if (machine == null)
		{
			LogWrapper.getLogger().info("No machine.");
			return;
		}
		JOptionPane.showMessageDialog(
				origin, 
				"Machine " + machine.getName() + " says \"" + message + "\"",
				"Message received on " + new Date(sent),
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

	public static void addChangeIdentifierMessage(String oldIdent, String newIdent, String ip, int port, String name, RSAPublicKey newKey)
	{
		Machine machine = DbMachines.getMachine(oldIdent);
		if (machine == null)
		{
			LogWrapper.getLogger().info("Unable to create change identifer message for " + oldIdent + " as we have no machine entry for it.");
			return;
		}
		if (!machine.getAllowsMessages())
		{
			LogWrapper.getLogger().info("Unable to create change identifer message for " + oldIdent + " as we not accepting messages from " + machine.getName());
			return;
		}
		
		String messageStr = new ChangeIdentifierUserMessage(
				newIdent,
				ip,
				port,
				newKey,
				name).toDebugString();
		if (messageStr.length() > UserMessage.MAX_MESSAGE_LENGTH)
		{
			LogWrapper.getLogger().info("The change identifer message is too long.");
			return;
		}
		UserMessage message = new UserMessage(machine, UserMessage.MessageType.CHANGE_IDENT.getDbValue(), messageStr);
		message.tryToSave();
		Services.notifications.messageReceived(message);
	}

	public static void addChangeKeyMessage(String url, String identifer, RSAPublicKey newKey)
	{
		Machine machine = DbMachines.getMachine(identifer);
		if (machine == null)
		{
			LogWrapper.getLogger().info("Unable to create change key message for " + identifer + " as we have no machine entry for it.");
			return;
		}
		if (!machine.getAllowsMessages())
		{
			LogWrapper.getLogger().info("Unable to create change key message for " + identifer + " as we not accepting messages from " + machine.getName());
			return;
		}
		
		String messageStr = new ChangeKeyUserMessageInfo(url, machine, newKey).toDebugString();
		if (messageStr.length() > UserMessage.MAX_MESSAGE_LENGTH)
		{
			LogWrapper.getLogger().info("The change key message is too long.");
			return;
		}
		UserMessage message = new UserMessage(machine, UserMessage.MessageType.CHANGE_KEYS.getDbValue(), messageStr);
		message.tryToSave();
		Services.notifications.messageReceived(message);
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
		TEXT(3),
		CHANGE_KEYS(4),
		CHANGE_IDENT(5),
		
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
