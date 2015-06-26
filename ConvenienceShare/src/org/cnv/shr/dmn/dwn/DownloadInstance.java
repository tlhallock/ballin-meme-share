
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



package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbChunks;
import org.cnv.shr.db.h2.DbChunks.DbChunk;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.ClientTrackerClient;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DownloadInstance implements Runnable
{
	static final Random random = new Random();
	static int NUM_PENDING_CHUNKS = 10;
	
	static long COMPLETION_REFRESH_RATE = 5 * 1000;
	
	private LinkedList<Seeder> freeSeeders = new LinkedList<Seeder>();
	private Map<Chunk, Seeder> pendingSeeders = new HashMap<Chunk, Seeder>();
	private boolean quit;
	
	
	private RemoteFile remoteFile;
	
	private Path destination;
	
	private Download download;
	
	
	DownloadInstance(Download d) throws IOException
	{
		this.download = d;
		remoteFile = d.getFile();
		Objects.requireNonNull(remoteFile.getChecksum(), "The remote file should already have a checksum.");
		setDestinationFile();
	}

	public synchronized void continueDownload()
	{
		Services.downloads.downloadThreads.execute(this);
	}
	
	public void run()
	{
		LogWrapper.getLogger().fine("Continuing download " + download);
		try
		{
			DownloadState state = download.getState();
			if (state.hasYetTo(DownloadState.REQUESTING_METADATA))
			{
				getMetaData();
				return;
			}

			requestSeeders();
			queue();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to continue download", e);
		}
	}

	private synchronized void getMetaData() throws UnknownHostException, IOException
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		LogWrapper.getLogger().info("Requesting more metadata");
		download.setState(DownloadState.REQUESTING_METADATA);
		
		if (DbChunks.hasAllChunks(download))
		{
			download.setState(DownloadState.DOWNLOADING);
			continueDownload();
			return;
		}
		
		FileRequest request = new FileRequest(remoteFile, download.getChunkSize());
		Machine machine = remoteFile.getRootDirectory().getMachine();
		Communication openConnection = Services.networkManager.openConnection(machine, false, "Download file");
		if (openConnection == null)
		{
			throw new IOException("Unable to authenticate to host.");
		}
		
		Seeder seeder = new Seeder(machine, openConnection);
		freeSeeders.addLast(seeder);
		seeder.send(request);

		// this should have a time out before it goes back to requesting meta data
		download.setState(DownloadState.RECEIVING_METADATA);
	}

	private void requestSeeders()
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		if (Math.random() < 2)
		{
			LogWrapper.getLogger().info("Currently not requesting seeders.");
			return;
		}
		
		Services.downloads.downloadThreads.execute(new Runnable() {
			@Override
			public void run()
			{
				LogWrapper.getLogger().info("Requesting seeders for " + download);
				LinkedList<Seeder> allSeeders = new LinkedList<>();
				allSeeders.addAll(freeSeeders);
				allSeeders.addAll(pendingSeeders.values());
				
				FileEntry fileEntry = remoteFile.getFileEntry();
				for (ClientTrackerClient client : Services.trackers.getClients())
				{
					client.requestSeeders(fileEntry, allSeeders);
				}
			}
		});
	}

	public synchronized void addSeeder(Machine machine, Communication connection)
	{
		freeSeeders.add(new Seeder(machine, connection));
		queue();
	}

	public synchronized void foundChunks(Machine machine, List<Chunk> chunks) throws IOException
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		Integer expected = remoteFile.getRootDirectory().getMachine().getId();
		Integer found = machine.getId();
		if (expected != found)
		{
			LogWrapper.getLogger().info("Machine id did not macth. Excepted " + expected + " found " + found);
			return;
		}
		download.setState(DownloadState.RECEIVING_METADATA);
		
		for (Chunk chunk : chunks)
		{
			if (chunk.getBegin() % download.getChunkSize() != 0)
			{
				LogWrapper.getLogger().info("Received a bad chunk!!: " + chunk + " chunksize is " + download.getChunkSize());
				continue;
			}
			DbChunks.addChunk(download, chunk);
			LogWrapper.getLogger().fine("Added chunk " + chunk);
		}
		
		if (DbChunks.hasAllChunks(download))
		{
			download.setState(DownloadState.DOWNLOADING);
		}

		continueDownload();
	}
	
	public synchronized void recover()
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		if (destination == null)
		{
			throw new RuntimeException("Trying to recover with no destination set!");
		}
		download.setState(DownloadState.RECOVERING);
		try (DbIterator<DbChunks.DbChunk> iterator = DbChunks.getAllChunks(download))
		{
			while (iterator.hasNext())
			{
				DbChunk next = iterator.next();
				LogWrapper.getLogger().info("Testing chunk " + next.chunk);
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
		catch (IOException | SQLException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to recover chunks.", e);
		}
		if (isDone())
		{
			download.setState(DownloadState.ALL_DONE);
		}
	}
	
	synchronized void allocate() throws IOException
	{
		if (!download.getState().hasYetTo(DownloadState.ALLOCATING) && Files.size(destination) == remoteFile.getFileSize())
		{
			return;
		}
		
		download.setState(DownloadState.ALLOCATING);
		try (RandomAccessFile raf = new RandomAccessFile(destination.toFile(), "rw");)
		{
			raf.setLength(remoteFile.getFileSize());
		}
	}
	
	private synchronized void queue()
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		LogWrapper.getLogger().info("Queue " + download);
		
		List<Chunk> upComing = DbChunks.getNextChunks(download, NUM_PENDING_CHUNKS - pendingSeeders.size());
		if (upComing.isEmpty())
		{
			checkIfDone();
			return;
		}
		if (freeSeeders.size() + pendingSeeders.size() == 0)
		{
			fail("There are no more seeders left!");
			return;
		}
		for (Chunk c : upComing)
		{
			if (pendingSeeders.containsKey(c))
			{
				continue;
			}
			if (freeSeeders.isEmpty())
			{
				LogWrapper.getLogger().info("All seeders busy.");
				break;
			}
			
			Seeder removeFirst = freeSeeders.removeFirst();
			try
			{
				boolean shouldCompress = c.getSize() > 50 && remoteFile.getPath().getUnbrokenName().endsWith(".txt");
				shouldCompress = true;
				removeFirst.request(remoteFile.getFileEntry(), c, shouldCompress);
				pendingSeeders.put(c, removeFirst);
				LogWrapper.getLogger().info("Requested chunk " + c + " from " + removeFirst.getConnection().getUrl());
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to request chunk", e);
			}
		}
	}

	private void checkIfDone()
	{
		if (!isDone())
		{
			return;
		}
		// This needs to happen a while later.
		// If this was the last chunk, then all seeders will be removed, including the one that inspired this queue (maybe a download chunk).
		// If we do it later, we can hope to terminate the connection peacefully.
		Services.downloads.downloadThreads.schedule(new Runnable()
		{
			@Override
			public void run()
			{
				complete();
			}
		}, 1, TimeUnit.SECONDS);
	}

	private boolean isDone()
	{
		return pendingSeeders.isEmpty() && DbChunks.hasAllChunks(download);
	}

	public synchronized void download(Chunk chunk, Communication connection, boolean compressed) throws IOException, NoSuchAlgorithmException
	{
		Seeder resquestedSeeder = pendingSeeders.remove(chunk);
		if (resquestedSeeder == null)
		{
			LogWrapper.getLogger().info("Some seeder gave an unknown chunk.");
			return;
		}
		freeSeeders.addLast(resquestedSeeder);
		
		resquestedSeeder.requestCompleted(null, chunk);
		connection.beginReadRaw();
		try
		{
			boolean successful = ChunkData.read(chunk, destination.toFile(), connection.getIn(), compressed);
			connection.endReadRaw();
			if (successful)
			{
				DbChunks.chunkDone(download, chunk, true);
			}
			connection.send(new CompletionStatus(remoteFile.getFileEntry(), getCompletionPercentage(true)));
		}
		catch (IOException e)
		{
			if (e.getMessage().contains("No space"))
			{
				Services.userThreads.execute(new Runnable() {
					@Override
					public void run()
					{
						JOptionPane.showConfirmDialog(null, "There is no space left on the filesystem!", "No space left", JOptionPane.ERROR_MESSAGE);
					}});
			}
			throw e;
		}
		
		queue();
	}
	
	private synchronized void complete()
	{
		for (Seeder seeder : freeSeeders)
		{
			seeder.done();
		}

		download.setState(DownloadState.VERIFYING_COMPLETED_DOWNLOAD);
		LogWrapper.getLogger().info("Verifying checksum of " + download);
		try
		{
			if (checkChecksum())
			{
				download.setState(DownloadState.ALL_DONE);
			}
			else
			{
				LogWrapper.getLogger().info("Checksums did not match!!!");
				download.setState(DownloadState.FAILED);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to calculate checksum", e);
			download.setState(DownloadState.FAILED);
		}


//		download.setState(DownloadState.PLACING_IN_FS);
//		try
//		{
//			Files.move(destination, download.getTargetFile(), StandardCopyOption.REPLACE_EXISTING);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to move downloaded file.", e);
//		}
		
		LocalDirectory local = getLocalDirectory();
		if (local != null)
		{
			try
			{
				LocalFile localFile = new LocalFile(local, remoteFile.getPath());
				DbPaths.pathLiesIn(remoteFile.getPath(), local);
				localFile.save(Services.h2DbCache.getThreadConnection());
			}
			catch (IOException | SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to add local file to local mirror " + remoteFile, e);
			}
		}
		
		Services.downloads.remove(this);
		Services.notifications.downloadDone(this);
	}

	private LocalDirectory getLocalDirectory()
	{
		Path localRoot = remoteFile.getRootDirectory().getLocalRoot();
		Misc.ensureDirectory(localRoot, true);
		
		LocalDirectory local = DbRoots.getLocal(localRoot.toString());
		if (local == null)
		{
			local = UserActions.addLocalImmediately(localRoot, remoteFile.getRootDirectory().getLocalMirrorName());
			if (local == null)
			{
				fail("Unable to create local mirror");
			}
		}
		return local;
	}

	public synchronized void setDestinationFile()
	{
		LocalDirectory localDirectory = getLocalDirectory();
		if (localDirectory == null)
		{
			return;
		}
		Path path = Paths.get(localDirectory.getPathElement().getFsPath());
		destination = PathSecurity.secureMakeDirs(path, Paths.get(remoteFile.getPath().getFullPath()));
		if (destination == null)
		{
			fail("Unable to get destination file " + destination);
		}
		download.setDestination(destination);
		
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

	public synchronized boolean contains(Communication c)
	{
		for (Seeder seeder : freeSeeders)
			if (seeder.is(c))
				return true;
		for (Seeder seeder : pendingSeeders.values())
			if (seeder.is(c))
				return true;
		return false;
	}
	
	private boolean checkChecksum() throws IOException
	{
		String checksumBlocking = Services.checksums.checksumBlocking(destination, Level.INFO);
		return checksumBlocking.equals(remoteFile.getChecksum());
	}

	private long lastCountRefresh;
	private double completionSatus;
	public double getCompletionPercentage(boolean force)
	{
		long now = System.currentTimeMillis();
		if (!force && lastCountRefresh + COMPLETION_REFRESH_RATE > now)
		{
			return completionSatus;
		}
		lastCountRefresh = now;
		return completionSatus = DbChunks.getDownloadPercentage(download);
	}
	
	public synchronized void sendCompletionStatus()
	{
		for (Seeder seeder : freeSeeders)
		{
			try
			{
				seeder.send(new CompletionStatus(remoteFile.getFileEntry(), getCompletionPercentage(false)));
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to send completion status.", e);
			}
		}
	}
	
	public synchronized void fail(String string)
	{
		LogWrapper.getLogger().info("Quiting download: " + string);
		quit = true;
		for (Seeder seeder : pendingSeeders.values())
			seeder.done();
		for (Seeder seeder : freeSeeders)
			seeder.done();
		Services.downloads.remove(this);
		Services.notifications.downloadRemoved(this);
		download.setState(DownloadState.FAILED);
	}

	public synchronized void removePeer(Communication connection)
	{
		Seeder badSeeder = null;
		for (Seeder seeder : freeSeeders)
		{
			if (seeder.is(connection))
			{
				badSeeder = seeder;
				freeSeeders.remove(seeder);
			}
		}
		for (Map.Entry<Chunk, Seeder> seeder : pendingSeeders.entrySet())
		{
			if (seeder.getValue().is(connection))
			{
				badSeeder = seeder.getValue();
				pendingSeeders.remove(seeder.getKey());
			}
		}
		if (badSeeder == null)
		{
			return;
		}

		if (freeSeeders.size() + pendingSeeders.size() == 0)
		{
			fail("There are no more seeders left!");
		}
		
		badSeeder.done();
	}

	public String getSpeed()
	{
		return "Speed will go here...";
	}
}
