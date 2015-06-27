package org.cnv.shr.gui.color;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public class ColorSetter
{
	final Properties properties = new Properties();
	final HashMap<String, LinkedList<ColorListener>> listeners = new HashMap<>();

	public void read()
	{
		Path path = Services.settings.colorsFile.getPath();
		try (InputStream input = Files.newInputStream(path))
		{
			properties.load(input);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read colors from " + path, e);
			store();
		}
	}

	public void store()
	{
		Path path = Services.settings.colorsFile.getPath();
		try (OutputStream input = Files.newOutputStream(path))
		{
			properties.store(input, null);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read colors from " + path, e);
		}
	}
	
	public void setColors(JFrame window)
	{
		ColorWindowListener windowListener = new ColorWindowListener(this);
		window.addWindowListener(windowListener);
		setColor(window.getRootPane(), windowListener, true);
		store();
	}

	private void setColor(JComponent component, ColorWindowListener stack, boolean addPopups)
	{
		ColorListener listener = new ColorListener(this, component);
		stack.add(listener);
		addPopups = addPopups && ColorUtils.shouldAddPopups(component) && addPopupMenu(listener);
		listener.updateColors();
		setChildrenColors(component, stack, addPopups);
	}

	private void setChildrenColors(JComponent component, ColorWindowListener stack, boolean addPopups)
	{
		if (!ColorUtils.descend(component))
		{
			return;
		}
		synchronized (component.getTreeLock())
		{
			for (Component c : component.getComponents())
			{
				if (c instanceof JComponent)
				{
					setColor((JComponent) c, stack, addPopups);
				}
			}
		}
	}

	private boolean addPopupMenu(ColorListener listener)
	{
		JComponent jc = listener.component;
		JPopupMenu menu = jc.getComponentPopupMenu();
		if (menu != null)
		{
			addMenuItems(menu, listener);
			return false;
		}
		JPopupMenu newmenu = new JPopupMenu();

		jc.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					newmenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					newmenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		addMenuItems(newmenu, listener);
		return true;
	}

	private void addMenuItems(final JPopupMenu newmenu, ColorListener component)
	{
		String listenerKey = component.getListenersKey();
		JMenuItem item = new JMenuItem("Set background for components like this...");
		item.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				MyColorChooser myColorChooser = new MyColorChooser(new NotifyingCallback(
						listenerKey, ColorUtils.getBackgroundKey(component.getListenersKey())) {
					@Override
					protected void notifyListeners(Color c)
					{
						for (ColorListener listener : listeners.get(listenerKey))
						{
							listener.setBackground(c);
						}
					}});
				myColorChooser.setTitle("Set background for all " + component.getClass().getName());
				myColorChooser.setLocation(component.component.getLocationOnScreen());
				myColorChooser.setVisible(true);
			}
		});
		newmenu.add(item);
		item = new JMenuItem("Set foreground for components like this...");
		item.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				MyColorChooser myColorChooser = new MyColorChooser(new NotifyingCallback(
						listenerKey, ColorUtils.getForegroundKey(component.getListenersKey())) {
					@Override
					protected void notifyListeners(Color c)
					{
						for (ColorListener listener : listeners.get(listenerKey))
						{
							listener.setForeground(c);
						}
					}});
				myColorChooser.setTitle("Set foreground for all " + component.getClass().getName());
				myColorChooser.setLocation(component.component.getLocationOnScreen());
				myColorChooser.setVisible(true);
			}
		});
		newmenu.add(item);
	}
	
	private abstract class NotifyingCallback implements ColorCallback
	{
		protected String listenerKey;
		protected String colorKey;

		public NotifyingCallback(String listenerKey, String colorKey)
		{
			this.listenerKey = listenerKey;
			this.colorKey = colorKey;
		}

		@Override
		public void colorChosen(Color c)
		{
			LogWrapper.getLogger().info("Setting " + colorKey + " to " + c);
	  	properties.setProperty(colorKey, ColorUtils.serializeColor(c));
			store();
			Services.userThreads.execute(new Runnable()
			{
				@Override
				public void run()
				{
					notifyListeners(c);
				}
			});
		}
		
		protected abstract void notifyListeners(Color c);
	}

	public void setAllBackgrounds(Color background)
	{
		for (Entry<String, LinkedList<ColorListener>> entry : listeners.entrySet())
		{
			properties.put(ColorUtils.getBackgroundKey(entry.getKey()), ColorUtils.serializeColor(background));
			for (ColorListener listener : entry.getValue())
			{
				listener.setBackground(background);
			}
		}
		store();
	}
	
	public void setAllForegrounds(Color foreground)
	{
		for (Entry<String, LinkedList<ColorListener>> entry : listeners.entrySet())
		{
			properties.put(ColorUtils.getForegroundKey(entry.getKey()), ColorUtils.serializeColor(foreground));
			for (ColorListener listener : entry.getValue())
			{
				listener.setForeground(foreground);
			}
		}
		store();
	}
}