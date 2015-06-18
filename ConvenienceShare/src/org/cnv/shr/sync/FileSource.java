
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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.FileOutsideOfRootException;

public interface FileSource
{
	boolean stillExists();
	FileSourceIterator listFiles() throws IOException, InterruptedException;
	String getName();
	boolean isDirectory();
	boolean isFile();
	String getCanonicalPath();
	long getFileSize();
	SharedFile create(RootDirectory local2, PathElement element) throws IOException, FileOutsideOfRootException;
	

	static FileSourceIterator NULL_ITERATOR = new FileSourceIterator()
	{
		@Override
		public boolean hasNext()
		{
			return false;
		}

		@Override
		public RemoteFileSource next()
		{
			return null;
		}

		@Override
		public void remove() {}

		@Override
		public void close() throws IOException {}
	};
	
	interface FileSourceIterator extends Iterator<FileSource>, Closeable {}
}
