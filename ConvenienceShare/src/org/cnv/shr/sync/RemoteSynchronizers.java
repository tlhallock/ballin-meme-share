
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



package org.cnv.shr.sync;

import java.io.IOException;
import java.util.Hashtable;

import javax.swing.JFrame;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.ConnectionParams.KeepOpenConnectionParams;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.util.WaitForObject;

public class RemoteSynchronizers
{
	public Hashtable<String, RemoteSynchronizerQueue> synchronizers = new Hashtable<>();
	
	private static String getKey(String url, RemoteDirectory r)
	{
		return url + "::" + r.getName();
	}
	
	public RemoteSynchronizerQueue getSynchronizer(Communication c, RemoteDirectory r)
	{
		return synchronizers.get(getKey(c.getUrl(), r));
	}

	public RemoteSynchronizerQueue createRemoteSynchronizer(JFrame origin, RemoteDirectory root) throws InterruptedException, IOException
	{
		WaitForObject<Communication> waiter = new WaitForObject<>(60 * 1000);
		
		Services.networkManager.openConnection(new KeepOpenConnectionParams(origin, root.getMachine(), false, "Synchronize directories") {
			@Override
			public void connectionOpened(Communication connection) throws Exception
			{
				waiter.set(connection);
			}
			public void onFail()
			{
				waiter.set(null);
			}
		});
		
		Communication communication = waiter.get();
		if (communication == null)
		{
			throw new IOException("Unable to open connection.");
		}
		RemoteSynchronizerQueue returnValue = new RemoteSynchronizerQueue(communication, root);
		synchronizers.put(getKey(communication.getUrl(), root), returnValue);
		return returnValue;
	}
	
	public void done(RemoteSynchronizerQueue sync)
	{
			synchronizers.remove(getKey(sync.getUrl(), sync.root));
	}

	public void closeAll()
	{
		for (RemoteSynchronizerQueue s : synchronizers.values())
		{
			s.close();
		}
	}
}
