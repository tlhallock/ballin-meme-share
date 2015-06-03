package org.cnv.shr.dmn.dwn;

import java.io.File;
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
import java.util.Random;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbChunks;
import org.cnv.shr.db.h2.DbChunks.DbChunk;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.TrackerClient;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;

public class DownloadInstance
{
	static final Random random = new Random();
	static int NUM_PENDING_CHUNKS = 10;
	static int CHUNK_SIZE = 1024 * 1024; // long numChunks = (remoteFile.getFileSize() / CHUNK_SIZE) + 1;
	static long COMPLETION_REFRESH_RATE = 5 * 1000;
	
	private HashMap<String, Seeder> seeders = new HashMap<>();
	private RemoteFile remoteFile;
	
	// TODO: native IO
	private Path tmpFile;
	
	private HashMap<String, ChunkRequest> pending = new HashMap<>();
	// These should be stored on file...
	private Download download;
	
	DownloadInstance(Download d)
	{
		this.download = d;
		d.setState(DownloadState.NOT_STARTED);
		remoteFile = d.getFile();
	}
	
	public synchronized void begin() throws UnknownHostException, IOException
	{
		if (!download.getState().equals(DownloadState.NOT_STARTED))
		{
			return;
		}
		setDestinationFile();
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
		for (TrackerClient client : Services.trackers.getClients())
		{
			client.requestSeeders(remoteFile, seeders.values());
		}
	}

	public void foundChunks(Machine machine, List<Chunk> chunks) throws IOException
	{
		if (remoteFile.getRootDirectory().getMachine().getId() != machine.getId())
		{
			return;
		}
		if (remoteFile.getChecksum() == null)
		{
			throw new RuntimeException("The remote file should already have a checksum.");
		}
		
		if (DbChunks.hasAllChunks(download, CHUNK_SIZE))
		{
			requestSeeders();
			allocate();
			recover();
			queue();
		}
	}
	
	private void recover()
	{
		download.setState(DownloadState.RECOVERING);
		try (DbIterator<DbChunks.DbChunk> iterator = DbChunks.getAllChunks(download))
		{
			while (iterator.hasNext())
			{
				DbChunk next = iterator.next();
				if (ChunkData.test(next.chunk, tmpFile))
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
	}
	
	private void allocate() throws IOException
	{
		download.setState(DownloadState.ALLOCATING);
		Path file = PathSecurity.getMirrorDirectory(remoteFile);
		
		// ensure that we are sharing this mirror...
		// This should use a better name...
		UserActions.addLocalImmediately(file, remoteFile.getRootDirectory().getLocalMirrorName());
		
		String str = remoteFile.getPath().getFsPath();
		tmpFile = PathSecurity.secureMakeDirs(file, Paths.get(str));
		if (tmpFile == null)
		{
			throw new FileOutsideOfRootException(file, str);
		}
		LogWrapper.getLogger().info("Downloading \"" + 
				remoteFile.getRootDirectory().getName() + ":" + remoteFile.getPath().getFullPath() + "\" to \"" +
				tmpFile + "\"");

		if (Files.size(tmpFile) > 0)
		{
			return;
		}
		
		// Should checkout random access file.setLength()
		try (OutputStream outputStream = Files.newOutputStream(tmpFile);)
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
					pending.put(c.getChecksum(), seeder.request(new SharedFileId(remoteFile), c));
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
		ChunkData.read(chunkRequest.getChunk(), tmpFile.toFile(), communication.getIn());
		pending.remove(chunk.getChecksum());
		DbChunks.chunkDone(download, chunk, true);
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
				LogWrapper.getLogger().info("Checksums did not match!!!");
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to calculate checksum", e);
		}
		
		try
		{
			Files.move(tmpFile, download.getTargetFile(), StandardCopyOption.REPLACE_EXISTING);
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
		Path destinationFile;
		File localRootF = remoteFile.getRootDirectory().getLocalRoot();
		Path localRoot = Paths.get(localRootF.getAbsolutePath());
		String str = remoteFile.getPath().getFsPath();
		Path ext = Paths.get(str);
		int index = 0;
		do
		{
			destinationFile = PathSecurity.secureMakeDirs(localRoot, ext);
			if (destinationFile == null)
			{
				fail("Unable to copy to destination...");
			}
			ext = Paths.get(str + "." + index++);
		} while (Files.exists(destinationFile));
		download.setDestination(destinationFile);
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
		return completionSatus = DbChunks.getDownloadPercentage(download);
	}
	
	public void sendCompletionStatus()
	{
		for (Seeder seeder : seeders.values())
		{
			try
			{
				seeder.send(new CompletionStatus(new SharedFileId(remoteFile), getCompletionPercentage()));
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