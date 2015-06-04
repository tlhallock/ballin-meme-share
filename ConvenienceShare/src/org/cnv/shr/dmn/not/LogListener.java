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
import org.cnv.shr.util.LogWrapper;

class LogListener implements NotificationListener
{
		@Override
		public void localsChanged()
		{
			LogWrapper.getLogger().info("Locals changed.");
		}

		@Override
		public void permissionFailure(PermissionFailureEvent event)
		{
			LogWrapper.getLogger().info("Permission failure: " + event);
		}

		@Override
		public void messageReceived(UserMessage message)
		{
			LogWrapper.getLogger().info("User message received: " + message);
		}

		@Override
		public void messagesChanged()
		{
			LogWrapper.getLogger().info("Messages changed.");
		}

		@Override
		public void localDirectoryChanged(LocalDirectory local)
		{
			LogWrapper.getLogger().info("Local changed: " + local);
		}

		@Override
		public void remoteChanged(Machine machine)
		{
			LogWrapper.getLogger().info("Remote changed: " + machine);
		}

		@Override
		public void remotesChanged()
		{
			LogWrapper.getLogger().info("Remotes changed.");
		}

		@Override
		public void remoteDirectoryChanged(RemoteDirectory remote)
		{
			LogWrapper.getLogger().info("Remote directory changed: " + remote);
		}

		@Override
		public void downloadAdded(DownloadInstance d)
		{
			LogWrapper.getLogger().info("New download " + d);
		}

		@Override
		public void downloadRemoved(DownloadInstance d)
		{
			LogWrapper.getLogger().info("Download removed: " + d);
		}

		@Override
		public void downloadDone(DownloadInstance d)
		{
			LogWrapper.getLogger().info("Download is done: " + d);
		}

		@Override
		public void serveAdded(ServeInstance s)
		{
			LogWrapper.getLogger().info("Now serving file: " + s);
		}

		@Override
		public void serveRemoved(ServeInstance s)
		{
			LogWrapper.getLogger().info("Done serving file: " + s);
		}

		@Override
		public void connectionOpened(Communication c)
		{
			LogWrapper.getLogger().info("Connection opened: " + c);
		}

		@Override
		public void connectionClosed(Communication c)
		{
			LogWrapper.getLogger().info("Connection closed: " + c);
		}

		@Override
		public void dbException(Exception ex)
		{
			LogWrapper.getLogger().info("Database exception.");
		}

		@Override
		public void fileAdded(SharedFile file)
		{
			LogWrapper.getLogger().fine("new file: " + file);
		}

		@Override
		public void fileChanged(SharedFile file)
		{
			LogWrapper.getLogger().info("File changed: " + file);
		}

		@Override
		public void fileDeleted(SharedFile file)
		{
			LogWrapper.getLogger().info("File gone: " + file);
		}
}
