package org.cnv.shr.dmn.dwn;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.ChecksumManager;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.dwn.ChunkList;
import org.cnv.shr.msg.dwn.ChunkResponse;
import org.cnv.shr.msg.dwn.DownloadFailure;
import org.cnv.shr.msg.dwn.RequestCompletionStatus;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.FileOutsideOfRootException;
import org.cnv.shr.util.LogWrapper;

public class ServeInstance extends TimerTask
{
	private LocalFile local;
	private Communication connection;
	
	private double lastCompletionPercentage;
	
	ServeInstance(Communication communication, LocalFile local)
	{
		this.local = local;
		this.connection = communication;
	}

	public Machine getMachine()
	{
		return connection.getMachine();
	}
	
	public SharedFile getFile()
	{
		return local;
	}
	
	private void serverChunks(int chunkSize) throws IOException, NoSuchAlgorithmException
	{
		local.ensureChecksummed();
		
		List<Chunk> chunks = new ArrayList<>(50);

		Path toShare = local.getFsFile();
		if (!local.getRootDirectory().contains(toShare))
		{
			// just to double check...
			throw new FileOutsideOfRootException(local.getRootDirectory().getPathElement().getFullPath(), toShare);
		}

		MessageDigest totalDigest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		byte[] buffer = new byte[1024];
		long offsetInFile = 0;

		try (InputStream inputStream = Files.newInputStream(toShare);)
		{
			boolean atEndOfFile = false;
			while (!atEndOfFile)
			{
				long chunkStart = offsetInFile;
				long chunkEnd = offsetInFile + chunkSize;

				int chunkOffset = 0;

				MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
				while (chunkOffset < chunkSize)
				{
					int nextReadSize = Math.min(buffer.length, chunkSize - chunkOffset);
					int nread = inputStream.read(buffer, 0, nextReadSize);

					if (nread < 0)
					{
						chunkEnd = chunkOffset;
						atEndOfFile = true;
						break;
					}

					chunkOffset += nread;
					offsetInFile += nread;
					digest.update(buffer, 0, nread);
					totalDigest.update(buffer, 0, nread);
				}

				Chunk chunk = new Chunk(chunkStart, chunkEnd, ChecksumManager.digestToString(digest));
				chunks.add(chunk);
				
				if (chunks.size() > 50)
				{
					connection.send(new ChunkList(chunks, local.getFileEntry()));
					chunks = new ArrayList<>(50);
				}
			}
		}

		connection.send(new ChunkList(chunks, local.getFileEntry()));
	}

	private void fail(String string)
	{
		LogWrapper.getLogger().info("Quiting download: " + string);
		try
		{
			connection.send(new DownloadFailure(string, local.getFileEntry()));
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to send failure reason.", e);
		}
		connection.finish();
	}

	public void sendChunks(int chunkSize)
	{
		try
		{
			LogWrapper.getLogger().info("Sending chunks of size " + chunkSize);
			serverChunks(chunkSize);
		}
		catch (NoSuchAlgorithmException e)
		{
			fail(e.getMessage());
			LogWrapper.getLogger().log(Level.SEVERE, "Algorithm not supported", e);
			Services.quiter.quit();
		}
		catch (IOException e)
		{
			fail(e.getMessage());
			LogWrapper.getLogger().log(Level.INFO, "Unable to send chunks", e);
		}
	}
	
	public void serve(Chunk chunk)
	{
		synchronized (connection.getOut())
		{
			try
			{
				LogWrapper.getLogger().info("Sending chunk " + chunk);
				connection.send(new ChunkResponse(local.getFileEntry(), chunk));
				// Right here I could check that the checksum matches...
				ChunkData.write(chunk, local.getFsFile(), connection.getOut());
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to write chunk.", e);
				fail("Unable to write chunk.");
			}
		}
	}
	
	public void quit()
	{
		connection.finish();
	}

	public void setPercentComplete(double percentComplete)
	{
		lastCompletionPercentage = percentComplete;
	}
	
	public double getCompletionPercentage()
	{
		return lastCompletionPercentage;
	}

	public boolean isServing(Communication c, LocalFile file)
	{
		return c.equals(connection);
	}

	@Override
	public void run()
	{
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					connection.send(new RequestCompletionStatus(local.getFileEntry()));
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Could not request status.", e);
					fail("Unable to request status.");
				}
			}
		});
	}
}
