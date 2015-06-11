
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.mdl.LocalFile;

public class ServeManager
{
	private HashMap<String, ServeInstance> serves = new HashMap<>();
	
	public synchronized ServeInstance getServeInstance(Communication communication)
	{
		return serves.get(communication.getUrl());
	}

	public synchronized ServeInstance serve(LocalFile file, Communication c)
	{
		for (ServeInstance instance : serves.values())
		{
			if (instance.isServing(c, file))
			{
				return instance;
			}
		}
		ServeInstance instance = new ServeInstance(c, file);
		serves.put(c.getUrl(), instance);
		Services.notifications.serveAdded(instance);
		return instance;
	}
	
	public synchronized void done(Communication c)
	{
		ServeInstance serveInstance = serves.get(c.getUrl());
		Services.notifications.serveRemoved(serveInstance);
		serves.remove(c.getUrl());
	}

	public synchronized List<ServeInstance> getServeInstances()
	{
		LinkedList<ServeInstance> returnValue = new LinkedList<>();
		returnValue.addAll(serves.values());
		return returnValue;
	}
}
