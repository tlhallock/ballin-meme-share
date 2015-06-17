
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



package org.cnv.shr.msg.dwn;

import java.io.IOException;
import java.io.InputStream;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.msg.Message;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.AbstractByteWriter;
import org.cnv.shr.util.ByteReader;

public abstract class DownloadMessage extends Message
{
	protected FileEntry descriptor;
	
	protected DownloadMessage() {}
	
	protected DownloadMessage(InputStream input) throws IOException
	{
		super(input);
	}
	
	protected DownloadMessage(FileEntry descriptor)
	{
		this.descriptor = descriptor;
	}
	
	@Override
	protected final void parse(ByteReader reader) throws IOException
	{
		String checksum = reader.readString();
		long fileSize = reader.readLong();
		descriptor = new FileEntry(checksum, fileSize);
		finishParsing(reader);
	}

	@Override
	protected final void print(Communication connection, AbstractByteWriter buffer) throws IOException
	{
		buffer.append(descriptor.getChecksum());
		buffer.append(descriptor.getFileSize());
		finishWriting(buffer);
	}
	
	protected abstract void finishParsing(ByteReader reader) throws IOException;
	protected abstract void finishWriting(AbstractByteWriter buffer) throws IOException;
	
	public LocalFile getLocal()
	{
		return DbFiles.getFile(descriptor.getChecksum(), descriptor.getFileSize());
	}

	protected FileEntry getDescriptor()
	{
		return descriptor;
	}
}
