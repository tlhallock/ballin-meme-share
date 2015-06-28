package org.cnv.shr.gui.color;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

class ColorWindowListener extends WindowAdapter
{
	LinkedList<ColorListener> children = new LinkedList<>();
	ColorSetter setter;
	
	ColorWindowListener(ColorSetter setter)
	{
		this.setter = setter;
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
	}
	
	@Override
	public void windowClosed(WindowEvent e)
	{
		for (ColorListener child : children)
		{
			child.remove();
		}
	}
}