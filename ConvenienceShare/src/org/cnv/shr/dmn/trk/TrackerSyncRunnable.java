package org.cnv.shr.dmn.trk;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbIterator;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.LogWrapper;

public class TrackerSyncRunnable implements Runnable
{
	private static final int QUEUE_SIZE = 100;
	TrackerClient client;
	
	LinkedList<FileEntry> toDel = new LinkedList<>();
	
	TrackerSyncRunnable(TrackerClient client)
	{
		this.client = client;
	}
	
	public void flush()
	{
		List<FileEntry> clone = (List<FileEntry>) toDel.clone();
		toDel.clear();

		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				delete(clone);
			}
		});
	}
	
	private void delete(List<FileEntry> clone)
	{
		try (TrackerConnection connection = client.connect(TrackerAction.LOSE_FILE);)
		{
			connection.generator.writeStartArray();
		  for (FileEntry entry : clone)
			{
				entry.print(connection.generator);
			}
			connection.generator.writeEnd();
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();
		}
		catch(InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove files. Giving up.", e);
			return;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove files.", e);
		}
	}

	public void delete(FileEntry entry)
	{
		toDel.add(entry);
		if (toDel.size() > QUEUE_SIZE)
		{
			flush();
		}
	}
	
	
	@Override
	public void run()
	{
		// should track which files already exist, or make sync action, should compress these...
		TrackerRequest request = new TrackerRequest(TrackerAction.LIST_MY_FILES);
		request.setParameter("other", Services.settings.machineIdentifier.get());
		try (TrackerConnection connection = client.connect(request))
		{
			FileEntry entry = new FileEntry();
			TrackObjectUtils.openArray(connection.parser);
			while (TrackObjectUtils.next(connection.parser, entry))
			{
				if (DbFiles.getFile(entry.getChecksum()) == null)
				{
					delete(entry);
				}
			}
			
			connection.generator.close();
		}
		catch(InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list files. Giving up.", e);
			return;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list stale files.", e);
		}
		
		flush();
		
		try (TrackerConnection connection = client.connect(TrackerAction.CLAIM_FILE);
				 DbIterator<LocalFile> locals = DbFiles.getChecksummedFiles();)
		{
			connection.generator.writeStartArray();
			while (locals.hasNext())
			{
				LocalFile next = locals.next();
				FileEntry entry = new FileEntry(next.getChecksum(), next.getFileSize());
				entry.print(connection.generator);
			}
			connection.generator.writeEnd();
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();
		}
		catch(InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to add files. Giving up.", e);
			return;
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to add files.", e);
		}
	}
}
