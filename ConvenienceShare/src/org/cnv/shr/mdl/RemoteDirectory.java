
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;

import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.PathSecurity;
import org.cnv.shr.sync.ConsecutiveDirectorySyncIterator;
import org.cnv.shr.sync.RemoteFileSource;
import org.cnv.shr.sync.RemoteSynchronizer;
import org.cnv.shr.sync.RemoteSynchronizerQueue;
import org.cnv.shr.sync.RootSynchronizer;


public class RemoteDirectory extends RootDirectory
{
	private Path path;
	private SharingState sharesWithUs;
	
	public RemoteDirectory(final Machine machine,
			final String name,
			final String tags, 
			final String description,
			final SharingState defaultShare)
	{
		super(machine, name, tags, description);
		
		path = Services.settings.downloadsDirectory.getPath().resolve(getLocalMirrorName()).toAbsolutePath();
		sharesWithUs = defaultShare;
	}

	public RemoteDirectory(final int int1)
	{
		super(int1);
		// just for now...
		DbPaths.pathLiesIn(DbPaths2.ROOT, this);
	}

	@Override
	public boolean isLocal()
	{
		return false;
	}
	
	@Override
	public String getPath()
	{
		return path.toString();
	}

	@Override
	protected void setPath(String path)
	{
		this.path = Paths.get(path);
	}

	public Path getLocalRoot()
	{
		return path;
	}
	
	public String getLocalMirrorName()
	{
		String string = "mirror" + "." + PathSecurity.getFsName(getName()) + "." + PathSecurity.getFsName(getMachine().getIdentifier());
		if (string.length() > MAX_DIRECTORY_NAME_LENGTH)
		{
			string = string.substring(0, MAX_DIRECTORY_NAME_LENGTH);
		}
		return string;
	}

	@Override
	protected RootSynchronizer createSynchronizer(JFrame origin) throws IOException, InterruptedException
	{
		final RemoteSynchronizerQueue createRemoteSynchronizer = Services.syncs.createRemoteSynchronizer(origin, this);
		final RemoteFileSource source = new RemoteFileSource(this, createRemoteSynchronizer);
		final ConsecutiveDirectorySyncIterator consecutiveDirectorySyncIterator = new ConsecutiveDirectorySyncIterator(this, source);
		consecutiveDirectorySyncIterator.setCloseable(createRemoteSynchronizer);
		return new RemoteSynchronizer(this, consecutiveDirectorySyncIterator);
	}
	
	@Override
	public boolean pathIsSecure(final Path canonicalPath)
	{
		return canonicalPath.startsWith(path);
	}

	@Override
	protected void sendNotifications()
	{
		Services.notifications.remoteDirectoryChanged(this);
	}

	@Override
	protected void setDefaultSharingState(SharingState sharingState)
	{
		this.sharesWithUs = sharingState;
	}
	
	public void setSharesWithUs(SharingState sharingState)
	{
		this.sharesWithUs = sharingState;
	}
	
	public SharingState getSharesWithUs()
	{
		return sharesWithUs;
	}
	@Override
	protected SharingState getDbSharing()
	{
		return sharesWithUs;
	}

	public void setLocalMirror(Path pathElement)
	{
		this.path = pathElement;
		tryToSave();
	}

	public RootDirectoryType getType()
	{
		return RootDirectoryType.REMOTE;
	}
}
