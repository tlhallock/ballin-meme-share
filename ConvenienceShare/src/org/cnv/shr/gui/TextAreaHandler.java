package org.cnv.shr.gui;

import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.swing.JTextArea;

public class TextAreaHandler extends Handler
{
	private int logLines;
    private LinkedList<String> logMessages = new LinkedList<>();
    private JTextArea logArea;
    private SimpleFormatter formatter;
    
    public TextAreaHandler(JTextArea area)
    {
    	this.formatter = new SimpleFormatter();
    	this.logArea = area;
    	this.logArea.setEditable(false);
    }

	private void log(String line)
	{
		logMessages.add(line);
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
	public void publish(LogRecord record)
	{
		for (String str : formatter.format(record).split("\n"))
		{
			log(str);
		}
	}
}
