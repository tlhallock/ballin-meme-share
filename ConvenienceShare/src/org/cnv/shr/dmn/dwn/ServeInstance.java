
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
import java.io.InputStream;
import java.io.OutputStream;
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
import org.cnv.shr.util.CompressionStreams2;
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
	
	public boolean dblCheckConnection()
	{
		if (connection.isClosed())
		{
			Services.server.done(connection);
			return false;
		}
		return true;
	}
	
	private void serveChunks(long chunkSize) throws IOException, NoSuchAlgorithmException
	{
		local.ensureChecksummed();
		
		List<Chunk> chunks = new ArrayList<>(50);

		Path toShare = local.getFsFile();
		if (!local.getRootDirectory().contains(toShare))
		{
			// just to double check...
			throw new FileOutsideOfRootException(local.getRootDirectory().getPath().toString(), toShare);
		}

		MessageDigest totalDigest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		int buffSize = 8192;
		if (chunkSize < buffSize)
		{
			buffSize = (int) chunkSize;
		}
		long fsize = Files.size(toShare);
		if (fsize < buffSize)
		{
			buffSize = (int) Files.size(toShare);
		}
		
		byte[] buffer = new byte[buffSize];
		long offsetInFile = 0;

		try (InputStream inputStream = Files.newInputStream(toShare);)
		{
			while (true)
			{
				boolean atEndOfFile = false;
				long chunkStart = offsetInFile;
				long chunkEnd = offsetInFile + chunkSize;

				long chunkOffset = 0;

				MessageDigest digest = MessageDigest.getInstance(Settings.checksumAlgorithm);
				while (chunkOffset < chunkSize)
				{
					int nextReadSize = buffer.length;
					if (nextReadSize > chunkSize - chunkOffset)
					{
						nextReadSize = (int) (chunkSize - chunkOffset);
					}
					int nread = inputStream.read(buffer, 0, nextReadSize);

					if (nread < 0)
					{
						chunkEnd = chunkStart + chunkOffset;
						atEndOfFile = true;
						break;
					}

					chunkOffset += nread;
					offsetInFile += nread;
					digest.update(buffer, 0, nread);
					totalDigest.update(buffer, 0, nread);
				}
				
				if (chunkStart != chunkEnd)
				{
					Chunk chunk = new Chunk(chunkStart, chunkEnd, ChecksumManager.digestToString(digest));
					chunks.add(chunk);
				}
				
				if (chunks.size() < 50 && !atEndOfFile)
				{
					continue;
				}

				connection.send(new ChunkList(chunks, local.getFileEntry()));
				if (atEndOfFile)
				{
					break;
				}
				chunks.clear();
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
		Services.server.done(connection);
	}

	private long lastChunksSent = 0;
	public void sendChunks(long chunkSize)
	{
		long now = System.currentTimeMillis();
		if (now < lastChunksSent + 10 * 1000)
		{
			LogWrapper.getLogger().info("Just sent chunks at " + lastChunksSent + ", now it is " + now + ".\nwaiting.");
			return;
		}
		lastChunksSent = now;
		
		try
		{
			LogWrapper.getLogger().info("Sending chunks of size " + chunkSize);
			serveChunks(chunkSize);
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
	
	public void serve(Chunk chunk, boolean compress)
	{
		synchronized (connection.getOutput())
		{
			try
			{
				compress |= Services.compressionManager.alwaysCompress(local.getPath().getUnbrokenName());
				LogWrapper.getLogger().info("Sending chunk " + chunk);
				connection.send(new ChunkResponse(local.getFileEntry(), chunk, compress));
				// Right here I could check that the checksum matches...

				if (compress)
				{
					try (OutputStream out = CompressionStreams2.newCompressedOutputStream(connection.getOutput()))
					{
						ChunkData.write(chunk, local.getFsFile(), out);
					}
				}
				else
				{
					connection.beginWriteRaw();
					ChunkData.write(chunk, local.getFsFile(), connection.getOutput());
					connection.endWriteRaw();
				}
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
		Services.downloads.downloadThreads.execute(() ->
		{
			if (!dblCheckConnection())
			{
				return;
			}
			try
			{
				connection.send(new RequestCompletionStatus(local.getFileEntry()));
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Could not request status.", e);
				fail("Unable to request status.");
			}
		});
	}
}
