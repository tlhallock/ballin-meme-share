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
