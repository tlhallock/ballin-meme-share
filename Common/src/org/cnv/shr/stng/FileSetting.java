
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



package org.cnv.shr.stng;

import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;

import org.cnv.shr.gui.ChangeFile;
import org.cnv.shr.util.Misc;

public class FileSetting extends Setting<File> 
{
	protected FileSetting(String n, File dv, boolean r, boolean u, String d)
	{
		super(n, dv, r, u, d);
	}

	@Override
	File parse(String vString)
	{
		return new File(vString);
	}

	@Override
	public Component createInput() {
		return new ChangeFile(this, JFileChooser.FILES_ONLY);
	}

	public Path getPath()
	{
		return Paths.get(get().getAbsolutePath());
	}
	
	public void set(Path path)
	{
		set(path.toFile());
		Misc.ensureDirectory(path, isFile());
	}

	protected boolean isFile()
	{
		return true;
	}
}
