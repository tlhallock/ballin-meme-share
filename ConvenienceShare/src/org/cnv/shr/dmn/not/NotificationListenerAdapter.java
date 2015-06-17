
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



package org.cnv.shr.dmn.not;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.msg.key.PermissionFailure.PermissionFailureEvent;

public abstract class NotificationListenerAdapter implements NotificationListener
{
	@Override
	public void localsChanged()                                        {}
	@Override
	public void permissionFailure(final PermissionFailureEvent event)  {}
	@Override
	public void messageReceived(final UserMessage message)             {}
	@Override
	public void messagesChanged()                                      {}
	@Override
	public void localDirectoryChanged(final LocalDirectory local)      {}
	@Override
	public void remoteChanged(final Machine machine)                   {}
	@Override
	public void remotesChanged()                                       {}
	@Override
	public void remoteDirectoryChanged(final RemoteDirectory remote)   {}
	@Override
	public void downloadAdded(final DownloadInstance d)                {}
	@Override
	public void downloadRemoved(final DownloadInstance d)              {}
	@Override
	public void downloadDone(final DownloadInstance d)                 {}
	@Override
	public void serveAdded(final ServeInstance s)                      {}
	@Override
	public void serveRemoved(final ServeInstance s)                    {}
	@Override
	public void connectionOpened(final Communication c)                {}
	@Override
	public void connectionClosed(final Communication c)                {}
	@Override
	public void dbException(final Exception ex)                        {}
	@Override
	public void fileAdded(final SharedFile file)                       {}
	@Override
	public void fileChanged(final SharedFile file)                     {}
	@Override
	public void fileDeleted(final SharedFile file)                     {}
}
