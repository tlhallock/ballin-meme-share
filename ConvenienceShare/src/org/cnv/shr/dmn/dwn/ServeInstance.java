package org.cnv.shr.dmn.dwn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.ChecksumManager;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.msg.dwn.ChunkList;
import org.cnv.shr.msg.dwn.ChunkResponse;
import org.cnv.shr.msg.dwn.DownloadFailure;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.util.FileOutsideOfRootException;

public class ServeInstance
{
	private LocalFile local;
	File tmpFile;
	int chunkSize;
	Communication connection;
	
	private double lastCompletionPercentage;
	
	// This should be stored on file...
	private HashMap<String, Chunk> chunks = new HashMap<>();
	
	ServeInstance(Communication communication, LocalFile local, int chunkSize)
	{
		this.local = local;
		this.connection = communication;
		this.chunkSize = chunkSize;
	}

	public Machine getMachine()
	{
		return connection.getMachine();
	}
	
	public SharedFile getFile()
	{
		return local;
	}
	
	private String stage() throws FileNotFoundException, IOException, NoSuchAlgorithmException
	{
		local.ensureChecksummed();
		
		tmpFile = PathSecurity.secureMakeDirs(Services.settings.servingDirectory.get(),
					local.getRootDirectory().getName()
					+ File.separator + local.getPath().getFullPath());
		File toShare = local.getFsFile();
		if (!local.getRootDirectory().contains(toShare.getCanonicalPath()))
		{
			// just to double check...
			throw new FileOutsideOfRootException(local.getRootDirectory().getPathElement().getFullPath(), toShare.getCanonicalPath());
		}

		MessageDigest totalDigest = MessageDigest.getInstance(Settings.checksumAlgorithm);
		
		byte[] buffer = new byte[1024];
		long offsetInFile = 0;
		
		try (   FileInputStream inputStream = new FileInputStream(toShare);
				FileOutputStream outputStream = new FileOutputStream(tmpFile);)
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
					outputStream.write(buffer, 0, nread);
				}

				Chunk chunk = new Chunk(chunkStart, chunkEnd, ChecksumManager.digestToString(digest), local.getChecksum());
				Chunk oldChunk = chunks.put(chunk.toString(), chunk);
				if (oldChunk != null)
				{
					fail("Duplicate chunk checksum");
				}
			}
		}

		return ChecksumManager.digestToString(totalDigest);
	}

	private void fail(String string)
	{
		System.out.println(string);
		connection.send(new DownloadFailure(string, new SharedFileId(local)));
		connection.finish();
	}

	public void sendChunks()
	{
		try
		{
			Services.logger.println("Staging.");
			String checksum = stage();
			Services.logger.println("Sending chunks.");
			connection.send(new ChunkList(chunks, checksum, new SharedFileId(local)));
		}
		catch (NoSuchAlgorithmException | IOException e)
		{
			Services.logger.print(e);
		}
	}
	
	public void serve(Chunk chunk)
	{
		Chunk myVersion = chunks.get(chunk.toString());
		if (!myVersion.equals(chunk))
		{
			// Should still send the chunk...
			fail("Unkown chunk");
			return;
		}

		synchronized (connection.getOut())
		{
			connection.send(new ChunkResponse(new SharedFileId(local), myVersion));
			try
			{
				ChunkData.write(myVersion, tmpFile, connection.getOut());
			}
			catch (IOException e)
			{
				Services.logger.print(e);
				fail("Unable to write chunk.");
			}
		}
	}
	
	public void quit()
	{
		tmpFile.delete();
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
	
	enum ServeState
	{
		STAGING,
		SERVING,
	}
}
