
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
import java.util.logging.Logger;

import org.cnv.shr.track.Track;
import org.cnv.shr.track.TrackerStore;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class TrackerTrackerClient extends TrackerClient
{
	TrackerStore store;
	
	public TrackerTrackerClient(TrackerEntry entry, TrackerStore store)
	{
		super(entry);
		this.store = store;
	}

	@Override
	protected TrackerConnection createConnection(int port) throws IOException
	{
		return new TrackerConnection(trackerEntry.getIp(), port)
		{
			@Override
			protected void sendDecryptedNaunce(byte[] naunceRequest, RSAPublicKey publicKey)
			{
				// No authentication
			}

			@Override
			protected MachineEntry getLocalMachine()
			{
				// No authentication
				return null;
			}
		};
	}

	@Override
	public void sync()
	{
		if (equals(Track.LOCAL_TRACKER))
		{
			return;
		}

		boolean hasMore = true;
		int offset = 0;
		while (hasMore)
		{
			hasMore = false;
			try (CloseableIterator<MachineEntry> list = list(offset);)
			{
				while (list.hasNext())
				{
					MachineEntry next = list.next();
					hasMore = true;
					store.machineFound(next, 0);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			offset += TrackerEntry.MACHINE_PAGE_SIZE;
		}
	}

	@Override
	protected void runLater(Runnable runnable)
	{
		Track.threads.execute(runnable);
	}

	@Override
	public void addOthers()
	{
		if (equals(Track.LOCAL_TRACKER))
		{
			return;
		}

		try (TrackerConnection connection = connect(TrackerAction.LIST_TRACKERS))
		{
			TrackerEntry other = new TrackerEntry();
			TrackObjectUtils.openArray(connection.parser);
			while (TrackObjectUtils.next(connection.parser, other))
			{
				store.addTracker(other);
			}
			connection.generator.writeEnd();
			connection.generator.flush();
			connection.parser.next();

			LogWrapper.getLogger().info("Added " + trackerEntry);
		}
		catch (Exception ex)
		{
			Logger.getLogger(TrackerClient.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
