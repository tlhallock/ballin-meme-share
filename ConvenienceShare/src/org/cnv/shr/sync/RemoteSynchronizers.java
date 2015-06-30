
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
import java.net.UnknownHostException;
import java.util.Hashtable;

import javax.swing.JFrame;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.RemoteDirectory;

public class RemoteSynchronizers
{
	public Hashtable<String, RemoteSynchronizerQueue> synchronizers = new Hashtable<>();
	
	private String getKey(RemoteSynchronizerQueue s)
	{
		return getKey(s.communication, s.root);
	}
	private String getKey(Communication c, RemoteDirectory r)
	{
		return c.getUrl() + "::" + r.getName();
	}
	
	public RemoteSynchronizerQueue getSynchronizer(Communication c, RemoteDirectory r)
	{
		return synchronizers.get(getKey(c, r));
	}

	public RemoteSynchronizerQueue createRemoteSynchronizer(JFrame origin, RemoteDirectory root) throws UnknownHostException, IOException
	{
		Communication c = Services.networkManager.openConnection(origin, root.getMachine(), false, "Synchronize directories");
		if (c == null)
		{
			throw new IOException("Unable to connect to remote!");
		}
		RemoteSynchronizerQueue returnValue = new RemoteSynchronizerQueue(c, root);
		synchronizers.put(getKey(returnValue), returnValue);
		return returnValue;
	}
	
	public void done(RemoteSynchronizerQueue sync)
	{
		synchronizers.remove(getKey(sync));
	}

	public void closeAll()
	{
		for (RemoteSynchronizerQueue s : synchronizers.values())
		{
			s.close();
		}
	}
}
