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
}