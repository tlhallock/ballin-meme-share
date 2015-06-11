
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


package org.cnv.shr.mdl;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.cnv.shr.db.h2.ConnectionWrapper;
import org.cnv.shr.db.h2.DbLocals;
import org.cnv.shr.db.h2.DbTables;


public class RemoteFile extends SharedFile
{
	public RemoteFile(int int1)
	{
		super(int1);
	}

	public RemoteFile(RemoteDirectory root, PathElement pathElement,
			long fileSize, String checksum, String tags, long lastModified)
	{
		super(null);
		rootDirectory = root;
		path = pathElement;
		this.fileSize = fileSize;
		this.checksum = checksum;
		this.tags = tags;
		this.lastModified = lastModified;
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
	
	public Path getTargetFile()
	{
		return Paths.get(
					getRootDirectory().getLocalRoot().toAbsolutePath().toString(),
					getPath().getFullPath());
	}

	@Override
	public RemoteDirectory getRootDirectory()
	{
		return (RemoteDirectory) super.getRootDirectory();
	}

	@Override
	protected RemoteDirectory fillRoot(ConnectionWrapper c, DbLocals locals, int rootId)
	{
		return (RemoteDirectory) locals.getObject(c, DbTables.DbObjects.RROOT, rootId);
	}
}
