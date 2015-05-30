package org.cnv.shr.stng;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

import org.cnv.shr.gui.ChangeFile;

public class DirectorySetting extends FileSetting
{
	protected DirectorySetting(String n, File dv, boolean r, boolean u, String d) {
		super(n, dv, r, u, d);
	}

	@Override
	public Component createInput() {
		return new ChangeFile(this, JFileChooser.DIRECTORIES_ONLY);
	}

	protected boolean isFile()
	{
		return false;
	}
}
