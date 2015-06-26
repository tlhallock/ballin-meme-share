
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

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.dwn.ChunkRequest;
import org.cnv.shr.trck.FileEntry;

public class Seeder
{
	
	Machine machine;
	int numRequests;
	int numSuccess;
	double bps;
	// Right now, only one download per peer...
	Communication connection;
	

	long lastRequest;
	
	
	Seeder(Machine machine, Communication openConnection)
	{
		this.machine = machine;
		this.connection = openConnection;
	}
	
	public ChunkRequest request(FileEntry descriptor, Chunk removeFirst, boolean compress) throws IOException
	{
		ChunkRequest msg = new ChunkRequest(descriptor, removeFirst, compress);
		lastRequest = System.currentTimeMillis();
		connection.send(msg);
		return msg;
	}

	public void requestCompleted(FileEntry descriptor, Chunk requested)
	{
	}
	
	public void send(Message message) throws IOException
	{
		connection.send(message);
	}

	public void done()
	{
		connection.finish();
	}

	public Communication getConnection()
	{
		return connection;
	}

	public boolean is(Machine remote)
	{
		return remote.getId() == machine.getId();
	}

	public boolean is(Communication c)
	{
		return connection.equals(c);
	}
}
