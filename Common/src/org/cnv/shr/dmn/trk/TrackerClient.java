
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



package org.cnv.shr.dmn.trk;

import java.io.IOException;
import java.util.logging.Level;

import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.trck.TrackerRequest;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;

public abstract class TrackerClient
{
	protected TrackerEntry trackerEntry;
	
	public TrackerClient(TrackerEntry entry)
	{
		this.trackerEntry = new TrackerEntry(entry);
	}

  protected TrackerConnection connect(TrackerAction action) throws Exception
  {
  	return connect(new TrackerRequest(action));
  }
  
	/**
	 * Throw exception. Json throws runtime exceptions. (Maybe only json exceptions?)
	 */
	TrackerConnection connect(TrackerRequest request) throws Exception
	{
		Exception lastException = null;
		TrackerConnection connection = null;
		for (int port = trackerEntry.getBeginPort(); port < trackerEntry.getEndPort(); port++)
		{
			try
			{
				connection = createConnection(port);
				break;
			}
			catch (IOException ex)
			{
				lastException = ex;
				LogWrapper.getLogger().log(Level.INFO, "Unable to connect on port " + port, ex);
			}
		}
		
		if (connection == null)
		{
			throw new IOException("Not able to connect on any ports.", lastException);
		}

		connection.connect(request);
		return connection;
	}

	protected abstract TrackerConnection createConnection(int port) throws IOException;

	public TrackerEntry getEntry()
	{
		return trackerEntry;
	}

	public String getAddress()
	{
		return trackerEntry.getAddress();
	}

	public CloseableIterator<MachineEntry> list(int start) throws Exception
	{
		TrackerRequest trackerRequest = new TrackerRequest(TrackerAction.LIST_ALL_MACHINES);
		trackerRequest.setParameter("offset", String.valueOf(start));
		TrackerConnection connection = connect(trackerRequest);
		TrackObjectUtils.openArray(connection.parser);
		CloseableIterator<MachineEntry> iterator = new CloseableIterator<MachineEntry>()
		{
			private MachineEntry next;

			{
				MachineEntry entry = new MachineEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
			}

			@Override
			public boolean hasNext()
			{
				return  next != null && !connection.socket.isClosed();
			}

			@Override
			public MachineEntry next()
			{
				MachineEntry returnValue = next;
				if (returnValue == null)
				{
					return null;
				}
				MachineEntry entry = new MachineEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
				return returnValue;
			}

			@Override
			public void close()
			{
				next = null;
				if (connection.socket.isClosed())
				{
					return;
				}
				try
				{
					connection.generator.writeEnd();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
				try
				{
					connection.close();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
			}
		};
		return iterator;
	}
	
	public interface CommentsListInterface extends CloseableIterator<CommentEntry>
	{
		public String getNumFiles();
	}

	CommentsListInterface listComments(MachineEntry machine) throws Exception
	{
		TrackerConnection connection = connect(TrackerAction.LIST_RATINGS);
		machine.generate(connection.generator, null);
		connection.generator.flush();
		
		NumFilesMessage numFilesMessage = new NumFilesMessage(connection.parser);
		
		TrackObjectUtils.openArray(connection.parser);
		CommentsListInterface iterator = new CommentsListInterface()
		{
			private CommentEntry next;
			{
				CommentEntry entry = new CommentEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
			}

			@Override
			public boolean hasNext()
			{
				return  next != null && !connection.socket.isClosed();
			}

			@Override
			public CommentEntry next()
			{
				CommentEntry returnValue = next;
				if (returnValue == null)
				{
					return null;
				}
				CommentEntry entry = new CommentEntry();
				if (connection.socket.isClosed() || !TrackObjectUtils.next(connection.parser, entry))
				{
					close();
				}
				else
				{
					next = entry;
				}
				return returnValue;
			}

			@Override
			public void close()
			{
				next = null;
				if (connection.socket.isClosed())
				{
					return;
				}
				try
				{
					connection.generator.writeEnd();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
				try
				{
					connection.close();
				}
				catch (Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to close", e);
				}
			}

			@Override
			public String getNumFiles()
			{
				Long numFiles = numFilesMessage.numFiles();
				if (numFiles == null)
				{
					return "0";
				}
				return String.valueOf(numFiles);
			}
		};
		return iterator;
	}

	void postComment(CommentEntry comment) throws Exception
	{
		try (TrackerConnection connection = connect(TrackerAction.POST_COMMENT))
		{
			comment.generate(connection.generator);
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();
			
			LogWrapper.getLogger().info("Posted comment " + comment);
		}
	}
	
	@Override
	public String toString()
	{
		return trackerEntry.toString();
	}

	public abstract void sync();
//	protected abstract void foundTracker(TrackerEntry entry);
  protected abstract void runLater(Runnable runnable);

	public abstract void addOthers();
	
	public boolean represents(TrackerEntry other)
	{
		return trackerEntry.equals(other);
	}
}
