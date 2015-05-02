package org.cnv.shr.dmn;

import java.awt.Container;
import java.util.LinkedList;
import java.util.Properties;

public abstract class Setting<T>
{
	private T value;
	private T defaultValue;
	private String display;
	private String name;
	private boolean requiresRestart;
	private boolean userEditable;
	private LinkedList<SettingListener> listeners;
	

	protected Setting(String n, T dv, boolean r, boolean u, String d) {
		value = dv;
		defaultValue = dv;
		name = n;
		display = d;
		requiresRestart = r;
		userEditable = u;
		listeners = new LinkedList<>();
	}
	
	public String toString()
	{
		return String.valueOf(get());
	}
	
	public synchronized void resetToDefaults()
	{
		value = defaultValue;
	}

	public synchronized void read(Properties p) {
		String vString = p.getProperty(name);
		if (vString == null)
		{
			value = defaultValue;
			return;
		}

		try
		{
			value = parse(vString);
		}
		catch (Exception e)
		{
			value = defaultValue;
		}
	}

	public synchronized void save(Properties p)
	{
		p.setProperty(name, String.valueOf(get()));
	}
	
	public synchronized void set(T t)
	{
		value = t;
		for (SettingListener l : listeners)
		{
			l.settingChanged();
		}
	}
	
	public synchronized void addListener(SettingListener l)
	{
		listeners.add(l);
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
	
	
	public interface SettingListener
	{
		void settingChanged();
	}

	public static class IntSetting extends Setting<Integer> {

		protected IntSetting(String n, Integer dv, boolean r, boolean u,
				String d) {
			super(n, dv, r, u, d);
		}

		@Override
		Integer parse(String vString) {
			return Integer.parseInt(vString);
		}

		@Override
		Container createInput() {
			return null;
		}
	}

	public static class LongSetting extends Setting<Long>
	{
		protected LongSetting(String n, Integer dv, boolean r, boolean u, String d) {
			super(n, (long) dv, r, u, d);
		}
		protected LongSetting(String n, Long dv, boolean r, boolean u, String d) {
			super(n, dv, r, u, d);
		}

		@Override
		Long parse(String vString) {
			return Long.parseLong(vString);
		}

		@Override
		Container createInput() {
			return null;
		}
	}

	public static class StringSetting extends Setting<String> {

		protected StringSetting(String n, String dv, boolean r, boolean u,
				String d) {
			super(n, dv, r, u, d);
		}

		@Override
		String parse(String vString) {
			return vString;
		}

		@Override
		Container createInput() {
			return null;
		}
	}
	
	public static class BooleanSetting extends Setting<Boolean>
	{
		protected BooleanSetting(String n, Boolean dv, boolean r, boolean u,
				String d) {
			super(n, dv, r, u, d);
		}

		@Override
		Boolean parse(String vString) {
			return Boolean.valueOf(vString);
		}

		@Override
		Container createInput() {
			return null;
		}
		
	}
}
