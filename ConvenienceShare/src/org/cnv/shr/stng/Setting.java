package org.cnv.shr.stng;

import java.awt.Container;
import java.util.LinkedList;
import java.util.Properties;

public abstract class Setting<T>
{
	protected T value;
	protected T defaultValue;
	protected String display;
	protected String name;
	protected boolean requiresRestart;
	protected boolean userEditable;
	protected LinkedList<SettingListener> listeners;
	
	protected Setting(String n, T dv, boolean r, boolean u, String d) 
	{
		listeners = new LinkedList<>();
		value = defaultValue = dv;
		name = n;
		display = d;
		requiresRestart = r;
		userEditable = u;
	}
	
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
	abstract Container createInput();
}
