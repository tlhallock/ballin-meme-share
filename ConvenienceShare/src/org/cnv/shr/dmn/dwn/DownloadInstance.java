package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbChunks;
import org.cnv.shr.db.h2.DbChunks.DbChunk;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.ClientTrackerClient;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DownloadInstance
{
	static final Random random = new Random();
	static int NUM_PENDING_CHUNKS = 10;
	static int CHUNK_SIZE = 1024 * 1024; // long numChunks = (remoteFile.getFileSize() / CHUNK_SIZE) + 1;
	static long COMPLETION_REFRESH_RATE = 5 * 1000;
	
	private HashMap<String, Seeder> seeders = new HashMap<>();
	private RemoteFile remoteFile;
	
	private Path destination;
	
	private HashMap<String, ChunkRequest> pending = new HashMap<>();
	// These should be stored on file...
	private Download download;
	
	DownloadInstance(Download d) throws IOException
	{
		this.download = d;
		remoteFile = d.getFile();
		Objects.requireNonNull(remoteFile.getChecksum(), "The remote file should already have a checksum.");
		setDestinationFile();
		allocate();
		recover();
	}

	public synchronized void continueDownload() throws UnknownHostException, IOException
	{
		DownloadState state = download.getState();
		if (state.hasYetTo(DownloadState.GETTING_META_DATA))
		{
			getMetaData();
			return;
		}
		
		queue();
		requestSeeders();
	}

	private void getMetaData() throws UnknownHostException, IOException
	{
		download.setState(DownloadState.GETTING_META_DATA);
		
		if (DbChunks.hasAllChunks(download, CHUNK_SIZE))
		{
			download.setState(DownloadState.DOWNLOADING);
			continueDownload();
			return;
		}
		
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

	private void requestSeeders()
	{
//		download.setState(DownloadState.FINDING_PEERS);
		Services.userThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				FileEntry fileEntry = remoteFile.getFileEntry();
				for (ClientTrackerClient client : Services.trackers.getClients())
				{
					client.requestSeeders(fileEntry, seeders.values());
				}
			}
		});
	}

	public void addSeeder(Machine machine, Communication connection)
	{
		seeders.put(connection.getUrl(), new Seeder(machine, connection));
		queue();
	}

	public void foundChunks(Machine machine, List<Chunk> chunks) throws IOException
	{
		if (remoteFile.getRootDirectory().getMachine().getId() != machine.getId())
		{
			return;
		}
		
		for (Chunk chunk : chunks)
		{
			DbChunks.addChunk(download, chunk);
		}

		Services.userThreads.execute(new Runnable() { @Override public void run() {
				try
				{
					continueDownload();
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to continue download.", e);
				}
			}});
		
		if (DbChunks.hasAllChunks(download, CHUNK_SIZE))
		{
			download.setState(DownloadState.DOWNLOADING);
		}
	}
	
	private void recover()
	{
		DownloadState prev = download.getState();
		download.setState(DownloadState.RECOVERING);
		try (DbIterator<DbChunks.DbChunk> iterator = DbChunks.getAllChunks(download))
		{
			while (iterator.hasNext())
			{
				DbChunk next = iterator.next();
				if (ChunkData.test(next.chunk, destination))
				{
					if (!next.done)
					{
						DbChunks.chunkDone(download, next.chunk, true);
					}
				}
				else if (next.done)
				{
					DbChunks.chunkDone(download, next.chunk, false);
				}
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "No algorithm", e);
			Services.quiter.quit();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to recover chunks.", e);
		}
		catch (SQLException e1)
		{
			e1.printStackTrace();
		}
		download.setState(prev);
	}
	
	private void allocate() throws IOException
	{
		if (!download.getState().hasYetTo(DownloadState.ALLOCATING) && Files.size(destination) > 0)
		{
			return;
		}
		download.setState(DownloadState.ALLOCATING);
		
		// Should checkout random access file.setLength()
		try (OutputStream outputStream = Files.newOutputStream(destination);)
		{
			for (long i = 0; i < remoteFile.getFileSize(); i++)
			{
				outputStream.write(0);
			}
		}
	}
	
	private synchronized void queue()
	{
		while (pending.size() < NUM_PENDING_CHUNKS)
		{
			List<Chunk> upComing = DbChunks.getNextChunks(download, NUM_PENDING_CHUNKS - pending.size());
			if (upComing.isEmpty())
			{
				if (pending.isEmpty())
				{
					complete();
				}
				return;
			}
			for (Chunk c : upComing)
			{
				if (seeders.isEmpty())
				{
					fail("There are no more seeders left!");
					return;
				}
				Seeder seeder = getRandomSeeder();
				try
				{
					pending.put(c.getChecksum(), seeder.request(remoteFile.getFileEntry(), c));
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to request chunk", e);
				}
			}
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
		// TODO: java nio
		ChunkData.read(chunkRequest.getChunk(), destination.toFile(), communication.getIn());
		pending.remove(chunk.getChecksum());
		DbChunks.chunkDone(download, chunk, true);
		communication.send(new CompletionStatus(remoteFile.getFileEntry(), getCompletionPercentage()));
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
				LogWrapper.getLogger().info("Checksums did not match!!!");
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to calculate checksum", e);
		}
		
		try
		{
			Files.move(destination, download.getTargetFile(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to move downloaded file.", e);
		}
		
		Services.downloads.remove(this);
		Services.notifications.downloadDone(this);
		download.setState(DownloadState.ALL_DONE);
		DbChunks.allChunksDone(download);
	}

	public void setDestinationFile()
	{
		Path localRoot = remoteFile.getRootDirectory().getLocalRoot();
		destination = PathSecurity.secureMakeDirs(localRoot, Paths.get(remoteFile.getPath().getFullPath()));
		if (destination == null)
		{
			fail("Unable to get destination file " + destination);
		}
		download.setDestination(destination);
		
		String localMirrorName = remoteFile.getRootDirectory().getLocalMirrorName();
		LocalDirectory local = DbRoots.getLocalByName(localMirrorName);
		if (local == null)
		{
			local = UserActions.addLocalImmediately(localRoot, localMirrorName);
			if (local == null)
			{
				fail("Unable to create local mirror");
			}
		}
		Misc.ensureDirectory(localRoot, true);
		LogWrapper.getLogger().info("Downloading \"" + remoteFile.getRootDirectory().getName() + ":" + remoteFile.getPath().getFullPath() + "\" to \"" + destination + "\"");
	}

	public Path getDestinationFile()
	{
		return download.getTargetFile();
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
		String checksumBlocking = Services.checksums.checksumBlocking(destination, Level.INFO);
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
		return completionSatus = DbChunks.getDownloadPercentage(download);
	}
	
	public void sendCompletionStatus()
	{
		for (Seeder seeder : seeders.values())
		{
			try
			{
				seeder.send(new CompletionStatus(remoteFile.getFileEntry(), getCompletionPercentage()));
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to send completion status to " + seeder.connection.getUrl(), e);
			}
		}
	}
	
	public void fail(String string)
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

	public String getSpeed()
	{
		return "Speed will go here...";
	}
}