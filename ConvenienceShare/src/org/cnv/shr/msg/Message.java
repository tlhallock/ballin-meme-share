
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



package org.cnv.shr.msg;

import java.io.IOException;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbPermissions;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.key.PermissionFailure;
import org.cnv.shr.util.Jsonable;

public abstract class Message implements Jsonable
{
	protected Message() {}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("Please implement toString() in class " + getClass().getName());
		return builder.toString();
	}
	
	public    abstract void perform(Communication connection) throws Exception;
	
	
	protected static void checkPermissionsVisible(Communication c, Machine machine, LocalDirectory root, String action) throws PermissionException, IOException
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
		c.finish();
		throw new PermissionException(action);
	}
	
	protected static void checkPermissionsDownloadable(Communication c, Machine machine, LocalDirectory root, String action) throws PermissionException, IOException
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
		c.finish();
		throw new PermissionException(action);
	}
	
	protected static void checkPermissionsViewable(Communication c, Machine machine, String action) throws PermissionException, IOException
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
		c.finish();
		throw new PermissionException(action);
	}

	public abstract String toDebugString();
	public abstract String getJsonKey();
}
