package org.cnv.shr.dmn.dwn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionParams.AutoCloseConnectionParams;
import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.QueryWrapper;
import org.cnv.shr.db.h2.ConnectionWrapper.StatementWrapper;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.not.NotificationListenerAdapter;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.ChecksumRequest;
import org.cnv.shr.util.LogWrapper;

public class ChecksumRequester extends Thread
{
	// TODO: go to database...
	
	private HashSet<SharedFileId> pending = new HashSet<>();
	private ConnectionWaiter waiter = new ConnectionWaiter();
	

	private static final QueryWrapper DELETE   = new QueryWrapper("delete CHK_REQ where FID=?;");
	private static final QueryWrapper ADD      = new QueryWrapper("merge into CHK_REQ key (FID) values (?);");
	private static final QueryWrapper GET      = new QueryWrapper("select FID from CHK_REQ;");
	
	{
		Services.notifications.add(waiter);
	}

	public void requestChecksum(RemoteFile remote)
	{
		LogWrapper.getLogger().info("File is not checksummed.");
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = connection.prepareStatement(ADD);)
		{
			stmt.setInt(1, remote.getId());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to add checksum request for " + remote, e);
		}
	}
	
	private static void removeRequest(int id)
	{
		try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
				StatementWrapper stmt = connection.prepareStatement(DELETE);)
		{
			stmt.setInt(1, id);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove checksum request for " + id, e);
		}
	}

	public boolean hasSharedPendingId(SharedFileId id)
	{
		return pending.remove(id);
	}

	public void fileHasChecksum(RemoteFile d)
	{
		waiter.fileHasChecksum(new SharedFileId(d));
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			try (ConnectionWrapper connection = Services.h2DbCache.getThreadConnection();
					StatementWrapper stmt = connection.prepareStatement(GET);
					ResultSet results = stmt.executeQuery();)
			{
				while (results.next())
				{
					RemoteFile file = (RemoteFile) DbFiles.getFile(results.getInt(1));
					if (file == null)
					{
						continue;
					}
					checksum(file);
				}
			}
			catch (SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to list checksum requests.", e);
			}
			
			try
			{
				Thread.sleep(10 * 1000);
			}
			catch (InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
				return;
			}
		}
	}
	
	private void checksum(RemoteFile remote)
	{
		try
		{
			SharedFileId fileid = new SharedFileId(remote);
			pending.add(fileid);
			LogWrapper.getLogger().info("Requesting checksum for " + fileid);
			Services.networkManager.openConnection(new AutoCloseConnectionParams(null, remote.getRootDirectory().getMachine(), false, "Request checksum") {
				@Override
				public void opened(Communication connection) throws Exception
				{
					waiter.setConnection(connection);
					connection.send(new ChecksumRequest(remote));
				}
				public void failed()
				{
					waiter.fileHasChecksum(fileid);
				}
			});
			
			waiter.waitForConnectionToClose(fileid);
			
			LogWrapper.getLogger().info("Done waiting for checksum of " + fileid);
			
			removeRequest(remote.getId());
		}
		catch (InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Interrupted, quitting.", e);
			return;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to send checksum request.", e);
		}
	}
	
	private class ConnectionWaiter extends NotificationListenerAdapter
	{
		private Communication openConnection;
		private ReentrantLock lock = new ReentrantLock();
		private Condition condition = lock.newCondition();
		private boolean requestCompleted;
		private SharedFileId fileId;
		
		public void waitForConnectionToClose(SharedFileId fileid) throws InterruptedException
		{
			this.fileId = fileid;
			requestCompleted = false;
			
			lock.lock();
			try
			{
				while (!requestCompleted && (openConnection == null || !openConnection.isClosed()))
				{
					LogWrapper.getLogger().info("Still waiting for checksum of " + fileid);
					condition.await(10, TimeUnit.SECONDS);
				}
			}
			finally
			{
				lock.unlock();
				openConnection = null;
				pending.remove(fileid);
			}
		}
		
		public void setConnection(Communication connection)
		{
			openConnection = connection;
		}

		public void connectionClosed(Communication c)
		{
			if (openConnection == null)
			{
				return;
			}
			if (!openConnection.equals(c))
			{
				return;
			}
			lock.lock();
			try
			{
				requestCompleted = true;
				condition.signalAll();
			}
			finally
			{
				lock.unlock();
			}
		}

		public void fileHasChecksum(SharedFileId sharedFileId)
		{
			if (!sharedFileId.equals(fileId))
			{
				return;
			}

			lock.lock();
			try
			{
				requestCompleted = true;
				condition.signalAll();
			}
			finally
			{
				lock.unlock();
			}
		};
	}
}
