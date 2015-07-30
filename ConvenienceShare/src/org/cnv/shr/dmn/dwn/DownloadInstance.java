
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
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbChunks;
import org.cnv.shr.db.h2.DbChunks.ChunkState;
import org.cnv.shr.db.h2.DbChunks.DbChunk;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadManager.GuiInfo;
import org.cnv.shr.dmn.trk.ClientTrackerClient;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.MirrorDirectory;
import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

/**
 *
 * DownloadManager lock must not be taken after download instance lock is taken.
 *
 */
public class DownloadInstance implements Runnable
{
	static final Random random = new Random();
	static int NUM_PENDING_CHUNKS = 10;
	
	static long COMPLETION_REFRESH_RATE = 5 * 1000;
	
	private LinkedList<Seeder> freeSeeders = new LinkedList<Seeder>();
	private Map<Chunk, Seeder> pendingSeeders = new HashMap<Chunk, Seeder>();
	private LinkedList<Seeder> readingSeeders = new LinkedList<Seeder>();
	private boolean quit;
	
	private RemoteFile remoteFile;
	private Path destination;
	private Integer downloadId;
	private Seeder primarySeeder;
	private long requestedMetaData;
	
	private static final long SEEDER_REQUEST_DELAY = 5 * 60 * 1000;
	private long lastSeederRequest;
	
	DownloadInstance(Download d, Seeder primarySeeder) throws IOException
	{
		Objects.requireNonNull(this.primarySeeder = primarySeeder, "Must have a primary seeder.");
		freeSeeders.addLast(primarySeeder);
		this.downloadId = d.getId();
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
		updateGui();
		Download download = getDownload();
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
		updateGui();
	}

	private synchronized void getMetaData() throws UnknownHostException, IOException
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		Download download = getDownload();
		LogWrapper.getLogger().info("Requesting more metadata for " + download);
		download.setState(DownloadState.REQUESTING_METADATA);
		
		if (DbChunks.hasAllChunks(download))
		{
			download.setState(DownloadState.DOWNLOADING);
			continueDownload();
			return;
		}

		if (requestedMetaData + 10 * 1000 > System.currentTimeMillis())
		{
			LogWrapper.getLogger().info("Already requested metadata.");
			return;
		}
		
		FileRequest request = new FileRequest(remoteFile, download.getChunkSize());

		primarySeeder.send(request);
		
		// this should have a time out before it goes back to requesting meta data
		download.setState(DownloadState.RECEIVING_METADATA);
		requestedMetaData = System.currentTimeMillis();
	}

	private void requestSeeders() throws UnknownHostException, IOException
	{
		if (quit)
		{
			LogWrapper.getLogger().info("Already quit.");
			return;
		}
		
		if (true)
		{
			return;
		}
		
		long now = System.currentTimeMillis();
		if (now - SEEDER_REQUEST_DELAY < lastSeederRequest)
		{
			LogWrapper.getLogger().info("Currently not requesting seeders.");
			return;
		}
		Download download = getDownload();

		DownloadInstance ins = this;
		lastSeederRequest = now;
		LogWrapper.getLogger().info("Requesting seeders for " + download);
		Services.downloads.downloadThreads.execute(() ->
		{
			LogWrapper.getLogger().info("Requesting seeders for " + download);
			LinkedList<Seeder> allSeeders = new LinkedList<>();
			
			synchronized (ins)
			{
				allSeeders.addAll(freeSeeders);
				allSeeders.addAll(readingSeeders);
				allSeeders.addAll(pendingSeeders.values());
			}
			
			FileEntry fileEntry = remoteFile.getFileEntry();
			for (ClientTrackerClient client : Services.trackers.getClients())
			{
				client.requestSeeders(fileEntry, allSeeders);
			}
			lastSeederRequest = System.currentTimeMillis();
		});
	}

	public synchronized void addSeeder(Machine machine)
	{
		// TODO:
//		freeSeeders.add(new Seeder(machine, connection));
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
		Download download = getDownload();
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
		Download download = getDownload();
		download.setState(DownloadState.RECOVERING);
		testCompletion(download);
		if (isDone())
		{
			download.setState(DownloadState.ALL_DONE);
		}
		else
		{
			download.setState(DownloadState.QUEUED);
		}
	}

	public static void testCompletion(Download downloadToTest)
	{
		if (!DbChunks.hasAllChunks(downloadToTest))
		{
			if (downloadToTest.getState().comesAfter(DownloadState.REQUESTING_METADATA))
			{
				downloadToTest.setState(DownloadState.QUEUED);
				return;
			}
		}
		
		boolean done = true;
		try (DbIterator<DbChunks.DbChunk> iterator = DbChunks.getAllChunks(downloadToTest))
		{
			while (iterator.hasNext())
			{
				DbChunk next = iterator.next();
				LogWrapper.getLogger().info("Testing chunk " + next.chunk);
				if (ChunkData.test(next.chunk, downloadToTest.getTargetFile()))
				{
					if (!next.state.isDone())
					{
						DbChunks.chunkDone(downloadToTest, next.chunk, ChunkState.DOWNLOADED);
					}
				}
				else
				{
					DbChunks.chunkDone(downloadToTest, next.chunk, ChunkState.NOT_DOWNLOADED);
					done = false;
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
		if (!done && downloadToTest.getState().equals(DownloadState.ALL_DONE))
		{
			downloadToTest.setState(DownloadState.QUEUED);
		}
	}
	
	synchronized void allocate() throws IOException
	{
		Download download = getDownload();
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

		Download download = getDownload();
		LogWrapper.getLogger().info("Queue " + download);
		
		dblCheckConnections();
		calculateSpeed();
		
		List<Chunk> upComing = DbChunks.getNextChunks(download, NUM_PENDING_CHUNKS - pendingSeeders.size());
		if (upComing.isEmpty())
		{
			checkIfDone();
			return;
		}
		if (getNumSeeders() == 0)
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
				// TODO: move size check
				removeFirst.request(remoteFile.getFileEntry(), c);
				pendingSeeders.put(c, removeFirst);
				DbChunks.chunkDone(download, c, ChunkState.REQUESTED);
				LogWrapper.getLogger().info("Requested chunk " + c + " from " + removeFirst.getConnection().getUrl());
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to request chunk", e);
			}
		}
	}

	private synchronized void dblCheckConnections()
	{
		for (Seeder seeder : (Iterable<Seeder>) freeSeeders.clone())
		{
			if (seeder.checkConnection())
			{
				continue;
			}
			freeSeeders.remove(seeder);
		}
		for (Seeder seeder : (Iterable<Seeder>) readingSeeders.clone())
		{
			if (seeder.checkConnection())
			{
				continue;
			}
			readingSeeders.remove(seeder);
		}
		LinkedList<Map.Entry<Chunk, Seeder>> list = new LinkedList<>();
		list.addAll(pendingSeeders.entrySet());
		for (Map.Entry<Chunk, Seeder> seeder : list)
		{
			if (seeder.getValue().checkConnection())
			{
				continue;
			}
			pendingSeeders.remove(seeder.getKey());
		}

		if (getNumSeeders() == 0)
		{
			fail("There are no more seeders left!");
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
		Services.downloads.downloadThreads.schedule(() ->
		{
			complete();
		}, 5 * 1000);
	}

	private boolean isDone()
	{
		Download download = getDownload();
		return pendingSeeders.isEmpty() && DbChunks.allChunksAreDone(download);
	}

	public void download(Chunk chunk, Communication connection) throws IOException, NoSuchAlgorithmException
	{
		Seeder resquestedSeeder;
		synchronized (this)
		{
			resquestedSeeder = pendingSeeders.remove(chunk);
			if (resquestedSeeder == null)
			{
				LogWrapper.getLogger().info("Some seeder gave an unknown chunk.");
				return;
			}
			resquestedSeeder.requestCompleted(null, chunk);
			readingSeeders.add(resquestedSeeder);
		}

		synchronized (resquestedSeeder)
		{
			try
			{
				boolean successful;

				connection.beginReadRaw();
				successful = ChunkData.read(chunk, destination.toFile(), connection.getIn());
				connection.endReadRaw();

				synchronized (this)
				{
					readingSeeders.remove(resquestedSeeder);
					freeSeeders.addLast(resquestedSeeder);
				}

				if (successful)
				{
					Download download = getDownload();
					DbChunks.chunkDone(download, chunk, ChunkState.DOWNLOADED);
				}
				connection.send(new CompletionStatus(remoteFile.getFileEntry(), getCompletionPercentage(true)));
			}
			catch (IOException e)
			{
				// should set state to error or remove seeder...
				if (e.getMessage().contains("No space"))
				{
					Services.userThreads.execute(() -> {
						JOptionPane.showConfirmDialog(null, "There is no space left on the filesystem!", "No space left", JOptionPane.ERROR_MESSAGE);
					});
				}
				throw e;
			}
		}

		queue();
	}

	private synchronized void complete()
	{
		for (Seeder seeder : freeSeeders)
		{
			seeder.done();
		}
		Download download = getDownload();

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

		MirrorDirectory local;
		try
		{
			local = getLocalDirectory();
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to get local directory", e1);
			fail("Unable to get mirror directory.");
			return;
		}
		if (local != null)
		{
			try
			{
				PathElement addFilePath = DbPaths2.addFilePath(local, remoteFile.getPath().getFullPath());
				LocalFile localFile = new LocalFile(addFilePath);
				localFile.save(Services.h2DbCache.getThreadConnection());
				
				Services.userThreads.execute(() ->
				{
					local.synchronize(null, null);
				});
			}
			catch (IOException | SQLException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to add local file to local mirror " + remoteFile, e);
			}
		}

		// Don't hold this lock...
		Services.downloads.downloadThreads.execute(() -> { Services.downloads.remove(remoteFile.getFileEntry()); });
		Services.downloads.removeGuiInfo(downloadId);
		Services.notifications.downloadDone(this);
	}

	private MirrorDirectory getLocalDirectory() throws IOException
	{
		Path localRoot = remoteFile.getRootDirectory().getLocalRoot();
		Misc.ensureDirectory(localRoot, true);
		
		MirrorDirectory local = (MirrorDirectory) DbRoots.getLocal(localRoot);
		if (local == null)
		{
			local = (MirrorDirectory) UserActions.addLocalImmediately(localRoot, remoteFile.getRootDirectory().getLocalMirrorName(), true);
			if (local == null)
			{
				fail("Unable to create local mirror");
			}
		}
		return local;
	}

	public synchronized void setDestinationFile() throws IOException
	{
		MirrorDirectory localDirectory = getLocalDirectory();
		if (localDirectory == null)
		{
			return;
		}
		Path path = localDirectory.getPath();
		destination = PathSecurity.secureMakeDirs(path, Paths.get(remoteFile.getPath().getFullPath()));
		if (destination == null)
		{
			fail("Unable to get destination file " + destination);
		}
		getDownload().setDestination(destination);
		
		LogWrapper.getLogger().info("Downloading \"" + remoteFile.getRootDirectory().getName() + ":" + remoteFile.getPath().getFullPath() + "\" to \"" + destination + "\"");
	}

	public Path getDestinationFile()
	{
		return getDownload().getTargetFile();
	}
	
	public Download getDownload()
	{
		Download download = DbDownloads.getDownload(downloadId);
		if (download == null)
		{
			fail("Lost download");
			throw new RuntimeException("Lost the download " + downloadId);
		}
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
		for (Seeder seeder : readingSeeders)
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
		return completionSatus = DbChunks.getDownloadPercentage(getDownload());
	}
	
	public void sendCompletionStatus(Communication c)
	{
		try
		{
			c.send(new CompletionStatus(remoteFile.getFileEntry(), getCompletionPercentage(false)));
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to send completion status.", e);
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
		for (Seeder seeder : readingSeeders)
			seeder.done();
		
		Download download = DbDownloads.getDownload(downloadId);
		if (download != null)
		{
			download.setState(DownloadState.FAILED);
		}
			
		Services.downloads.downloadThreads.execute(() -> { Services.downloads.remove(remoteFile.getFileEntry()); });
		Services.downloads.removeGuiInfo(downloadId);
		Services.notifications.downloadRemoved(this);
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
		for (Seeder seeder : readingSeeders)
		{
			if (seeder.is(connection))
			{
				badSeeder = seeder;
				readingSeeders.remove(seeder);
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

		if (getNumSeeders() == 0)
		{
			fail("There are no more seeders left!");
		}
		
		badSeeder.done();
	}

	private String speed = "0 bps";
	public  String getSpeed()
	{
		return speed;
	}
	private synchronized void calculateSpeed()
	{
		double speedD = 0;
		for (Seeder seeder : pendingSeeders.values())
		{
			speedD += seeder.getConnection().getStatistics().getSpeedDown();
		}
		for (Seeder seeder : freeSeeders)
		{
			speedD += seeder.getConnection().getStatistics().getSpeedDown();
		}
		for (Seeder seeder : readingSeeders)
		{
			speedD += seeder.getConnection().getStatistics().getSpeedDown();
		}
		speed =  Misc.formatDiskUsage(speedD) + "ps"; 
	}

	public int getNumSeeders()
	{
		return freeSeeders.size() + pendingSeeders.size() + readingSeeders.size();
	}
	
	private void updateGui()
	{
		Services.downloads.updateGuiInfo(getDownload(), new GuiInfo(
				String.valueOf(getNumSeeders()),
				getSpeed(),
				String.valueOf(getCompletionPercentage(false) * 100)));
	}
}
