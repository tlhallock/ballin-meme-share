package org.cnv.shr.dmn.dwn;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.LookingFor;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.util.FileOutsideOfRootException;

public class DownloadInstance
{
	static final Random random = new Random();
	static int NUM_PENDING_CHUNKS = 10;
	static int CHUNK_SIZE = 1024 * 1024; // long numChunks = (remoteFile.getFileSize() / CHUNK_SIZE) + 1;
	static long COMPLETION_REFRESH_RATE = 5 * 1000;
	
	private HashMap<String, Seeder> seeders = new HashMap<>();
	private RemoteFile remoteFile;
	private File tmpFile;
	private File destinationFile;
	
	private HashMap<String, ChunkRequest> pending = new HashMap<>();
	// These should be stored on file...
	private LinkedList<Chunk> completed = new LinkedList<>();
	private LinkedList<Chunk> upComing;
	private Download download;
	
	DownloadInstance(Download d)
	{
		this.download = d;
		d.setState(DownloadState.NOT_STARTED);
		remoteFile = d.getFile();
	}
	
	private long count(Collection<Chunk> chunks)
	{
		long returnValue = 0;
		for (Chunk c : chunks)
		{
			returnValue += c.getSize();
		}
		return returnValue;
	}
	
	public synchronized void begin() throws UnknownHostException, IOException
	{
		if (!download.getState().equals(DownloadState.NOT_STARTED))
		{
			return;
		}
		download.setState(DownloadState.GETTING_META_DATA);
		FileRequest request = new FileRequest(remoteFile, CHUNK_SIZE);
		Machine machine = remoteFile.getRootDirectory().getMachine();
		Communication openConnection = Services.networkManager.openConnection(machine, false);
		if (openConnection == null)
		{
			throw new IOException("Unable to authenticate to host.");
		}
		
		Seeder seeder = new Seeder(machine, openConnection);
		seeders.put(openConnection.getUrl(), seeder);
		seeder.send(request);
	}

	public void addSeeder(Machine machine, Communication connection)
	{
		seeders.put(connection.getUrl(), new Seeder(machine, connection));
		queue();
	}

	private void requestSeeders()
	{
		download.setState(DownloadState.FINDING_PEERS);
		DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();
		
		outer:
		while (listRemoteMachines.hasNext())
		{
			final Machine remote = listRemoteMachines.next();
			
			for (Seeder seeder : seeders.values())
			{
				if (seeder.is(remote))
				{
					continue outer;
				}
			}
			
			Services.userThreads.execute(new Runnable() {
				@Override
				public void run()
				{
					try
					{
						Communication openConnection = Services.networkManager.openConnection(remote, false);
						if (openConnection == null)
						{
							return;
						}
						openConnection.send(new LookingFor(remoteFile));
						openConnection.finish();
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						Services.logger.print(e);
					}
				}});
		}
	}

	public void foundChunks(LinkedList<Chunk> chunks) throws IOException
	{
		if (remoteFile.getChecksum() == null)
		{
//			remoteFile.setChecksum(checksum);
			try
			{
				remoteFile.save();
			}
			catch (SQLException e)
			{
				Services.logger.print(e);
			}
			throw new RuntimeException("The remote file should already have a checksum.");
		}
		
		upComing = chunks;
		for (Chunk c : upComing)
		{
			if (c.getSize() > Services.settings.maxChunkSize.get())
			{
				
			}
		}
		requestSeeders();
		allocate();
		recover();
		queue();
	}
	
	private void recover()
	{
		download.setState(DownloadState.RECOVERING);
		for (Chunk chunk : upComing)
		{
			try
			{
				if (ChunkData.test(chunk, tmpFile))
				{
					completed.add(chunk);
				}
			}
			catch (Exception e)
			{
				Services.logger.print(e);
			}
		}
		for (Chunk c : completed)
		{
			upComing.remove(c);
		}
	}
	
	private void allocate() throws IOException
	{
		download.setState(DownloadState.ALLOCATING);
		File file = PathSecurity.getMirrorDirectory(remoteFile);
		
		// ensure that we are sharing this mirror...
		// This should use a better name...
		UserActions.addLocal(file, false, null);
		
		String str = remoteFile.getPath().getFullPath();
		tmpFile = PathSecurity.secureMakeDirs(file, str);
		if (tmpFile == null)
		{
			throw new FileOutsideOfRootException(file.getAbsolutePath(), str);
		}
		Services.logger.println("Downloading \"" + 
				remoteFile.getRootDirectory().getName() + ":" + remoteFile.getPath().getFullPath() + "\" to \"" +
				tmpFile.getAbsolutePath() + "\"");

		if (tmpFile.length() > 0)
		{
			return;
		}
		try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tmpFile));)
		{
			for (long i = 0; i < remoteFile.getFileSize(); i++)
			{
				outputStream.write(0);
			}
		}
	}
	
	private void queue()
	{
		while (pending.size() < NUM_PENDING_CHUNKS && !upComing.isEmpty())
		{
			if (seeders.isEmpty())
			{
				return;
			}
			Seeder seeder = getRandomSeeder();
			Chunk next = upComing.removeFirst();
			pending.put(next.getChecksum(), seeder.request(new SharedFileId(remoteFile), next));
		}
		
		if (upComing.isEmpty() && pending.isEmpty())
		{
			complete();
		}
	}

	private Seeder getRandomSeeder()
	{
		if (seeders.size() == 1)
		{
			return seeders.entrySet().iterator().next().getValue();
		}
		int index = random.nextInt(seeders.size() - 1);
		Iterator<Entry<String, Seeder>> iterator = seeders.entrySet().iterator();
		for (int i = 0; i < index; i++)
		{
			iterator.next();
		}
		return iterator.next().getValue();
	}

	public synchronized void download(Chunk chunk, Communication communication) throws IOException, NoSuchAlgorithmException
	{
		ChunkRequest chunkRequest = pending.get(chunk.getChecksum());
		if (chunkRequest == null)
		{
			fail("Unkown chunk.");
			return;
		}
		ChunkData.read(chunkRequest.getChunk(), tmpFile, communication.getIn());
		pending.remove(chunk.getChecksum());
		completed.add(chunk);
		communication.send(new CompletionStatus(new SharedFileId(remoteFile), getCompletionPercentage()));
		queue();
	}
	
	private void complete()
	{
		for (Seeder seeder : seeders.values())
		{
			seeder.done();
		}

		download.setState(DownloadState.PLACING_IN_FS);
		try
		{
			if (!checkChecksum())
			{
				Services.logger.println("Checksums did not match!!!");
			}
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
		

		try
		{
			Files.move(Paths.get(tmpFile.getAbsolutePath()), Paths.get(getDestinationFile().getAbsolutePath()));
		}
		catch (IOException e)
		{
			Services.logger.print(e);
		}
		
		Services.downloads.remove(this);
		Services.notifications.downloadDone(this);
		download.setState(DownloadState.ALL_DONE);
	}

	public File getDestinationFile()
	{
		if (destinationFile == null)
		{
			File localRoot = ((RemoteDirectory) remoteFile.getRootDirectory()).getLocalRoot();
			String str = remoteFile.getPath().getFullPath();
			do
			{
				destinationFile = PathSecurity.secureMakeDirs(localRoot, str);
				if (destinationFile == null)
				{
					fail("Unable to copy to destination...");
				}
				str = str + ".new";
			} while (destinationFile.exists());
		}
		return destinationFile;
	}
	
	public Download getDownload()
	{
		return download;
	}
	
	public String getChecksum()
	{
		return remoteFile.getChecksum();
	}

	public boolean contains(Communication c)
	{
		return seeders.get(c.getUrl()) != null;
	}
	
	private boolean checkChecksum() throws IOException
	{
		String checksumBlocking = Services.checksums.checksumBlocking(tmpFile);
		return checksumBlocking.equals(remoteFile.getChecksum());
	}

	private long lastCountRefresh;
	private double completionSatus;
	public double getCompletionPercentage()
	{
		long now = System.currentTimeMillis();
		if (lastCountRefresh + COMPLETION_REFRESH_RATE < now)
		{
			return completionSatus;
		}
		lastCountRefresh = now;
		long done = count(completed);
		long comingUp = count(upComing);
		for (ChunkRequest r : pending.values())
		{
			comingUp += r.getChunk().getSize();
		}
		
		return completionSatus = done / (double) (comingUp + done);
	}
	
	public void sendCompletionStatus()
	{
		for (Seeder seeder : seeders.values())
		{
			seeder.send(new CompletionStatus(new SharedFileId(remoteFile), getCompletionPercentage()));
		}
	}
	
	void fail(String string)
	{
		System.out.println(string);
		for (Seeder seeder : seeders.values())
		{
			seeder.done();
		}
		Services.downloads.remove(this);
		Services.notifications.downloadRemoved(this);
	}

	public void removePeer(Communication connection)
	{
		Seeder seeder = seeders.remove(connection.getUrl());
		if (seeder != null)
		{
			seeder.done();
		}
		
		if (seeders.isEmpty())
		{
			fail("There are no more seeders left!");
		}
	}
}