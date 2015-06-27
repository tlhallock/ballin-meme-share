
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
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.JComponent;

import org.cnv.shr.util.LogWrapper;

public abstract class Setting<T>
{
	protected T value;
	protected T defaultValue;
	protected String display;
	protected String name;
	protected boolean requiresRestart;
	protected boolean userEditable; // need to return null if this is false.
	protected LinkedList<SettingListener> listeners;
	
	private SettingsEditor editor;
	
	protected Setting(String n, T dv, boolean r, boolean u, String d) 
	{
		listeners = new LinkedList<>();
		value = defaultValue = dv;
		name = n;
		display = d;
		requiresRestart = r;
		userEditable = u;
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}
	
	public synchronized void resetToDefaults()
	{
		set(defaultValue);
	}

	public synchronized void read(Properties p) {
		String vString = p.getProperty(name);
		if (vString == null)
		{
			resetToDefaults();
			return;
		}

		try
		{
			set(parse(vString));
		}
		catch (Exception e)
		{
			resetToDefaults();
		}
	}

	public synchronized void save(Properties p)
	{
		p.setProperty(name, String.valueOf(get()));
	}
	
	public synchronized void set(T t)
	{
		synchronized(this)
		{
			if (t == null || value.equals(t))
			{
				return;
			}
			value = t;
			sanitize();
		}
		notifyListeners();
	}
	
	protected void sanitize() {}

	protected void notifyListeners()
	{
		LogWrapper.getLogger().info("Set " + getName() + " to " + String.valueOf(get()));
		for (SettingListener l : listeners)
		{
			l.settingChanged();
		}
	}
	
	public synchronized void addListener(SettingListener l)
	{
		if (!listeners.contains(l))
		{
			listeners.add(l);
		}
	}
	
	public synchronized void removeListener(SettingListener l)
	{
		listeners.remove(l);
	}

	public synchronized T get()
	{
		if (value == null)
		{
			value = defaultValue;
		}
		return value;
	}

	abstract T parse(String vString);
	protected abstract JComponent createInput();

	public SettingsEditor getEditor()
	{
		if (editor == null)
		{
			editor = new SettingsEditor(createInput());
		}
		return editor;
	}

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return String.valueOf(display);
    }

    public boolean requiresReboot()
    {
        return requiresRestart;
    }
    
    public class SettingsEditor
    {
    	JComponent c;
    	
    	SettingsEditor(JComponent c)
    	{
    		this.c = c;
    	}
    	
    	public boolean isEditable()
    	{
    		return userEditable;
    	}
    	
    	public Component get()
    	{
    		return c;
    	}
    }
}
