
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
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.msg.Wait;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class ServeManager
{
	private static final int REFRESH_PERIOD = 10 * 1000;
	
	private Hashtable<String, ServeInstance> serves = new Hashtable<>();
	
	public synchronized ServeInstance getServeInstance(Communication communication)
	{
		return serves.get(communication.getUrl());
	}

	public synchronized ServeInstance serve(LocalFile file, Communication c) throws IOException
	{
		for (ServeInstance instance : serves.values())
		{
			if (instance.isServing(c, file))
			{
				return instance;
			}
		}
		
		if (serves.size() >= Services.settings.maxSimServers.get())
		{
			Services.userThreads.execute(() -> {
				synchronized (serves)
				{
					for (ServeInstance instance : serves.values())
					{
						instance.dblCheckConnection();
					}
				}
			});
			
			LogWrapper.getLogger().info("Over number of simultaneous servers.");
			c.send(new Wait());
			c.finish();
			return null;
		}
		
		ServeInstance instance = new ServeInstance(c, file);
		serves.put(c.getUrl(), instance);
		Misc.timer.scheduleAtFixedRate(instance, REFRESH_PERIOD, REFRESH_PERIOD);
		Services.notifications.serveAdded(instance);
		return instance;
	}
	
	public synchronized void done(Communication c)
	{
		ServeInstance serveInstance = serves.get(c.getUrl());
		Services.notifications.serveRemoved(serveInstance);
		serves.remove(c.getUrl());
		serveInstance.cancel();
	}

	public synchronized List<ServeInstance> getServeInstances()
	{
		LinkedList<ServeInstance> returnValue = new LinkedList<>();
		returnValue.addAll(((Map<String, ServeInstance>) serves.clone()).values());
		return returnValue;
	}
}
