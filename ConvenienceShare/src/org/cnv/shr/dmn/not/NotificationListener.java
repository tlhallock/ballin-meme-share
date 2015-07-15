
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

public interface NotificationListener
{
	void localsChanged()                                        ;
	void permissionFailure(final PermissionFailureEvent event)  ;
	void messageReceived(final UserMessage message)             ;
	void messagesChanged()                                      ;
	void localDirectoryChanged(final LocalDirectory local)      ;
	void remoteChanged(final Machine machine)                   ;
	void remotesChanged()                                       ;
	void remoteDirectoryChanged(final RemoteDirectory remote)   ;
	void downloadAdded(final DownloadInstance d)                ;
	void downloadRemoved(final DownloadInstance d)              ;
	void downloadDone(final DownloadInstance d)                 ;
	void serveAdded(final ServeInstance s)                      ;
	void serveRemoved(final ServeInstance s)                    ;
	void connectionOpened(final Communication c)                ;
	void connectionClosed(final Communication c)                ;
	void dbException(final Exception ex)                        ;
	void fileAdded(final SharedFile file)                       ;
	void fileChanged(final SharedFile file)                     ;
	void fileDeleted(final SharedFile file)                     ;
	void permissionsChanged(final Machine remote)               ;
	void permissionsChanged(final RemoteDirectory remote)       ;
	void downloadsChanged()                                     ;
}
