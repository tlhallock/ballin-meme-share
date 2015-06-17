
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class UpdateHeaders
{
	private static final Path[] rootDirs = new Path[]
	{
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\ConvenienceShare\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Common\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Updater\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Installer\\src"),
//		Paths.get("C:\\Users\\thallock\\Documents\\Source\\ballin-meme-share\\Tracker\\src"),
		

		Paths.get("/work/ballin-meme-share/ConvenienceShare/src"),
		Paths.get("/work/ballin-meme-share/Common/src"),
		Paths.get("/work/ballin-meme-share/Updater/src"),
		Paths.get("/work/ballin-meme-share/Installer/src"),
		Paths.get("/work/ballin-meme-share/Tracker/src"),
	};
	
	public static void main(String[] args) throws IOException
	{
		for (Path root : rootDirs)
		{
			Files.walkFileTree(root, new FileVisitor<Path>()
			{
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
				{
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					if (!file.toString().endsWith(".java"))
					{
						return FileVisitResult.CONTINUE;
					}
					updateHeader(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
				{
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
				{
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
	private static void updateHeader(Path file) throws IOException
	{
		Path original = file;
		Path backup   = Paths.get(original.toString() + ".new");
		
		try (PrintStream output = new PrintStream(Files.newOutputStream(backup));
				 BufferedReader input  = Files.newBufferedReader(original);)
		{
			int state = 0;

			printHeader(output);
			
			String line;
			while ((line = input.readLine()) != null)
			{
				switch (state)
				{
				case 0: // waiting
					if (line.contains("/*"))
					{
						state = 1;
						// continue to case 1
					}
					else if (line.trim().length() != 0)
					{
						state = 2;
						output.println(line);
						break;
					}
					else
					{
						output.println();
						break;
					}
				case 1: // skipping
					if (line.contains("*/"))
					{
						state = 2;
						line = input.readLine();
						if (line.trim().length() != 0)
						{
							// one extra line in header...
							output.println(line);
						}
					}
					break;
				case 2: // finishing
					output.println(line);
					break;
				}
			}
		}
		Files_move(backup, original);
	}

	private static void printHeader(PrintStream output)
	{
		printHeader(output, null);
	}

	private static void printHeader(PrintStream output, String filename)
	{
		output.println();
		output.println("/*                                                                          *");
//		output.println("** " + filename + " **");
		output.println(" * Copyright (C) 2015    Trever Hallock                                     *");
		output.println(" *                                                                          *");
		output.println(" * This program is free software; you can redistribute it and/or modify     *");
		output.println(" * it under the terms of the GNU General Public License as published by     *");
		output.println(" * the Free Software Foundation; either version 2 of the License, or        *");
		output.println(" * (at your option) any later version.                                      *");
		output.println(" *                                                                          *");
		output.println(" * This program is distributed in the hope that it will be useful,          *");
		output.println(" * but WITHOUT ANY WARRANTY; without even the implied warranty of           *");
		output.println(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *");
		output.println(" * GNU General Public License for more details.                             *");
		output.println(" *                                                                          *");
		output.println(" * You should have received a copy of the GNU General Public License along  *");
		output.println(" * with this program; if not, write to the Free Software Foundation, Inc.,  *");
		output.println(" * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *");
		output.println(" *                                                                          *");
		output.println(" * See LICENSE file at repo head at                                         *");
		output.println(" * https://github.com/tlhallock/ballin-meme-share                           *");
		output.println(" * or after                                                                 *");
		output.println(" * git clone git@github.com:tlhallock/ballin-meme-share.git                 */");
		output.println();
	}
	
	
	public static void Files_move(Path origin, Path dest) throws IOException
	{
//		Files.move(backup, original, StandardCopyOption.REPLACE_EXISTING);
		try (OutputStream output = Files.newOutputStream(dest);
				 InputStream input  = Files.newInputStream(origin);)
		{
			while (true)
			{
				int read = input.read();
				if (read < 0)
				{
					break;
				}
				output.write(read);
			}
		}
		
		Files.delete(origin);
	}
}
