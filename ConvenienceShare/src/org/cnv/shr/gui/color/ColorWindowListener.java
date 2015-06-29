package org.cnv.shr.gui.color;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

import javax.swing.JFrame;

class ColorWindowListener extends WindowAdapter
{
	LinkedList<ColorListener> children = new LinkedList<>();
	JFrame window;
	ColorSetter setter;
	
	ColorWindowListener(ColorSetter setter, JFrame window)
	{
		this.setter = setter;
		this.window = window;
	}
	
	void add(ColorListener listener)
	{
		String key = listener.component.getClass().getName();
		children.add(listener);
		LinkedList<ColorListener> linkedList = setter.listeners.get(key);
		if (linkedList == null)
		{
			linkedList = new LinkedList<>();
			setter.listeners.put(key, linkedList);
		}
		linkedList.add(listener);
		setter.colorListeners.put(listener.component, listener);
	}
	
	@Override
	public void windowClosed(WindowEvent e)
	{
		synchronized (setter.windowListeners)
		{
			setter.windowListeners.remove(window);
		}
		synchronized (this)
		{
			for (ColorListener child : children)
			{
				child.remove();
			}
		}
	}
}