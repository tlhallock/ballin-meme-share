package org.cnv.shr.gui.color;

import java.awt.Color;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;

import org.cnv.shr.gui.KeyPanel;

class ColorListener
{
	JComponent component;
	ColorSetter setter;
	private LinkedList<ColorListener> children = new LinkedList<>();

	ColorListener(ColorSetter setter, JComponent component2)
	{
		this.setter = setter;
		component = component2;
	}

	void remove()
	{
		setter.listeners.get(component.getClass().getName()).remove(this);
		setter.colorListeners.remove(component);
	}
	
	void setOpaque(boolean isOpaque)
	{
		component.setOpaque(isOpaque);
	}

	void setForeground(Color color)
	{
		component.setForeground(color);
		Border border = component.getBorder();
		if (border instanceof TitledBorder)
		{
			TitledBorder b = (TitledBorder) border;
			b.setTitleColor(color);
		}
		if (component instanceof JTable)
		{
			JTable table = (JTable) component;
			JTableHeader tableHeader = table.getTableHeader();
			tableHeader.setOpaque(true);
			tableHeader.setForeground(color);
		}
	}

	void setBackground(Color color)
	{
		component.setBackground(color);
		if (component instanceof JSplitPane)
		{
			JSplitPane pane = (JSplitPane) component;
			ColorSplitPaneUI ui;

			if (pane.getUI() instanceof ColorSplitPaneUI)
			{
				ui = (ColorSplitPaneUI) pane.getUI();
			}
			else
			{
				ui = new ColorSplitPaneUI(color);
				pane.setUI(ui);
			}

			ui.myBackground = color;
			pane.repaint();
		}
		if (component instanceof JTable)
		{
			JTable table = (JTable) component;
			JTableHeader tableHeader = table.getTableHeader();
			tableHeader.setOpaque(true);
			tableHeader.setBackground(color);
		}
		if (component instanceof KeyPanel)
		{
			((KeyPanel) component).setColor();
		}
	}

	private Color getBackground()
	{
		String foregroundKey = ColorUtils.getBackgroundKey(getListenersKey());
		Color returnValue = ColorUtils.getColor(setter.properties.getProperty(foregroundKey));
		if (returnValue != null)
		{
			return returnValue;
		}
		returnValue = component.getBackground();
		setter.properties.put(foregroundKey, ColorUtils.serializeColor(returnValue));
		return returnValue;
	}

	private Color getForeground()
	{
		String foregroundKey = ColorUtils.getForegroundKey(getListenersKey());
		Color returnValue = ColorUtils.getColor(setter.properties.getProperty(foregroundKey));
		if (returnValue != null)
		{
			return returnValue;
		}
		returnValue = component.getForeground();
		setter.properties.put(foregroundKey, ColorUtils.serializeColor(returnValue));
		return returnValue;
	}

	void updateColors()
	{
		setBackground(getBackground());
		setForeground(getForeground());
	}

	String getListenersKey()
	{
		if (component instanceof JSplitPane)
		{
			return JPanel.class.getName();
		}
		return component.getClass().getName();
	}
	
	
	void addChild(ColorListener child)
	{
		children.add(child);
	}
	void removeAllChildren()
	{
		for (ColorListener child : children)
		{
			child.remove();
			child.removeAllChildren();
		}
		children.clear();
	}
}
