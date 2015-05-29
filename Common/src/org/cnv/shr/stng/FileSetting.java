package org.cnv.shr.stng;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

import org.cnv.shr.gui.ChangeFile;

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
}
