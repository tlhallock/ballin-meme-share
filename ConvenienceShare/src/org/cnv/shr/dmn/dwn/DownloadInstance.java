package org.cnv.shr.dmn.dwn;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.RemoteFile;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.msg.dwn.CompletionStatus;
import org.cnv.shr.msg.dwn.FileRequest;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.FileSystem;

public class DownloadInstance
{
	static final Random random = new Random();
	static int NUM_PENDING_CHUNKS = 10;
	static long CHUNK_SIZE = 1024 * 1024;
	static long COMPLETION_REFRESH_RATE = 5 * 1000;
	
	private DownloadState state;
	
	private LinkedList<Seeder> seeders = new LinkedList<>();
	private RemoteFile remoteFile;
	private File tmpFile;
	
	private HashMap<String, ChunkRequest> pending = new HashMap<>();
	// These should be stored on file...
	private LinkedList<Chunk> completed = new LinkedList<>();
	private LinkedList<Chunk> upComing;
	
	DownloadInstance(RemoteFile remote)
	{
		state = DownloadState.NOT_STARTED;
		remoteFile = remote;
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
	
	public Communication begin() throws UnknownHostException, IOException
	{
		state = DownloadState.GETTING_META_DATA;
		FileRequest request = new FileRequest(remoteFile, (remoteFile.getFileSize() / CHUNK_SIZE) + 1);
		Machine machine = remoteFile.getRootDirectory().getMachine();
		Communication openConnection = Services.networkManager.openConnection(machine.getIp() + ":" + machine.getPort());
		Seeder seeder = new Seeder(machine, openConnection);
		seeders.add(seeder);
		seeder.send(request);
		requestSeeders();
		return openConnection;
	}

	private void requestSeeders()
	{
		state = DownloadState.FINDING_PEERS;
		DbIterator<Machine> listRemoteMachines = DbMachines.listRemoteMachines();
		while (listRemoteMachines.hasNext())
		{
			Machine next = listRemoteMachines.next();
		}
	}

	public void foundChunks(LinkedList<Chunk> chunks) throws IOException
	{
		upComing = chunks;
		for (Chunk c : upComing)
		{
			if (c.getSize() > Services.settings.maxChunkSize.get())
			{
				
			}
		}
		allocate();
		recover();
		allocate();
		queue();
	}
	
	private void recover()
	{
		state = DownloadState.RECOVERING;
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
				e.printStackTrace();
			}
		}
		for (Chunk c : completed)
		{
			upComing.remove(c);
		}
	}
	
	private void allocate() throws IOException
	{
		state = DownloadState.ALLOCATING;
		File file = Services.settings.stagingDirectory.get();
		String str = remoteFile.getPath().getFullPath();
		tmpFile = PathSecurity.secureMakeDirs(file, str);
		if (tmpFile == null)
		{
			throw new FileOutsideOfRootException(file.getAbsolutePath(), str);
		}

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
			Seeder seeder = seeders.get(random.nextInt(seeders.size()));
			Chunk next = upComing.removeFirst();
			pending.put(next.getChecksum(), seeder.request(next));
		}
		
		if (upComing.isEmpty() && pending.isEmpty())
		{
			complete();
		}
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
		communication.send(new CompletionStatus(getCompletionPercentage()));
		queue();
	}
	
	private void complete()
	{
		state = DownloadState.PLACING_IN_FS;
		try
		{
			checkChecksum();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		File localRoot = ((RemoteDirectory) remoteFile.getRootDirectory()).getLocalRoot();
		String str = remoteFile.getPath().getFullPath();
		File outFile = PathSecurity.secureMakeDirs(localRoot, str);
		if (outFile == null)
		{
			fail("Unable to copy...");
		}

		try
		{
			FileSystem.move(tmpFile, outFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		for (Seeder seeder : seeders)
		{
			seeder.done();
		}
	}
	
	private boolean checkChecksum() throws IOException
	{
		String checksumBlocking = Services.checksums.checksumBlocking(tmpFile);
		return checksumBlocking.equals(remoteFile.getChecksum());
	}
	
	public DownloadState getCurrentState()
	{
		return state;
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
		for (Seeder seeder : seeders)
		{
			seeder.send(new CompletionStatus(getCompletionPercentage()));
		}
	}
	
	private void fail(String string)
	{
		System.out.println(string);
	}
	
	enum DownloadState
	{
		NOT_STARTED,
		GETTING_META_DATA,
		FINDING_PEERS,
		RECOVERING,
		ALLOCATING,
		DOWNLOADING,
		PLACING_IN_FS,
	}
}