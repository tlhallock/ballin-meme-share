
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


package org.cnv.shr.inst;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.TextAreaHandler;


public class MonitorThread extends Thread
{
	private Process start;
	private TextAreaHandler textAreaHandler;
	
	
	public MonitorThread(Process start, Rectangle bounds)
	{
		this.start = start;
		JFrame frame = new JFrame("Starting ConvenienceShare...");
		frame.setBounds(bounds);
		JTextArea area = new JTextArea();
		frame.getContentPane().add(area);
		LogWrapper.getLogger().addHandler(textAreaHandler = new TextAreaHandler(area, 500));
		frame.setVisible(true);
		
	}

	public void run()
	{
		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(start.getInputStream()));)
		{
			String line;
			while ((line = inputStream.readLine()) != null)
			{
				System.out.println(line);
				LogWrapper.getLogger().info(line);
				if (line.contains(Misc.INITIALIZED_STRING))
				{
					LogWrapper.getLogger().info("DONE!");
					System.exit(0);
				}
			}
			JOptionPane.showMessageDialog(null, 
					"Unable to confirm process start.",
					"Unable to start ConvenienceShare",
					JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to start new process", e);
		}
	};
}
