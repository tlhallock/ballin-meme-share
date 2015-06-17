
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



package org.cnv.shr.dmn.mn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;

import org.cnv.shr.stng.Settings;

public class Arguments
{
	// needs to have a quitting and testing...
	public boolean connectedToTestStream = false;
	public boolean deleteDb = false;
	public boolean showGui = false;
	public Settings settings = new Settings(Settings.DEFAULT_SETTINGS_FILE);
	public Quiter quiter = new Quiter() {
		@Override
		public void doFinal()
		{
			System.exit(0);
		}};
	String testIp;
	String testPort;
	
	public String updateManagerDirectory;
	

	void parseArgs(String[] args) throws FileNotFoundException, IOException
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals("-d"))
			{
				deleteDb = true;
			}
			if (args[i].equals("-g"))
			{
				showGui = true;
			}
			if (args[i].equals("-f") && i < args.length - 1)
			{
				settings = new Settings(Paths.get(args[i + 1]));
			}

			if (args[i].equals("-u") && i < args.length - 1)
			{
				File directory = new File(args[i+1]);
				if (!directory.exists() || !new File(args[i+1]).isDirectory())
				{
					continue;
				}
				updateManagerDirectory = args[i + 1];
			}
		}
	}
}
