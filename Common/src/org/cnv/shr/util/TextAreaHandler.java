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
		
		System.out.println("Currently at " + builder.toString());
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
	public void publish(LogRecord record)
	{
		String message = record.getMessage();

		if (record.getThrown() != null)
		{
			StringWriter out = new StringWriter();
			record.getThrown().printStackTrace(new PrintWriter(out));
			message = message + "\n" + out.toString();
		}
		for (String str : message.split("\n"))
		{
			log(str);
		}
	}
}
