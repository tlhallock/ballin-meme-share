
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


package org.cnv.shr.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JTextArea;

public class TextAreaHandler extends Handler
{
		private int logLines;
    private LinkedList<String> logMessages = new LinkedList<>();
    private JTextArea logArea;
    
    public TextAreaHandler(JTextArea area, int initialNumLines)
    {
    	this.logArea = area;
    	this.logArea.setEditable(false);
    	logLines = initialNumLines;
    }

	private void log(String line)
	{
		logMessages.add(line + '\n');
		while (logMessages.size() > logLines)
		{
			logMessages.removeFirst();
		}
		StringBuilder builder = new StringBuilder();
		for (String s : logMessages)
		{
			builder.append(s);
		}
		logArea.setText(builder.toString());
	}
	
	public void setLogLines(int numLines)
	{
		this.logLines = numLines;
	}

	@Override
	public void close() {}

	@Override
	public void flush() {}

	@Override
	public synchronized void publish(LogRecord record)
	{
		String message = record.getMessage();

		Throwable thrown = record.getThrown();
		if (thrown != null)
		{
			StringWriter out = new StringWriter();
			try (PrintWriter s = new PrintWriter(out);)
			{
				thrown.printStackTrace(s);
			}
			message = message + "\n" + out.toString();
		}
		for (String str : message.split("\n"))
		{
			log(str);
		}
	}
}
