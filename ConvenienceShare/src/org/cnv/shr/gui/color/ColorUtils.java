package org.cnv.shr.gui.color;

import java.awt.Color;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.cnv.shr.util.LogWrapper;

class ColorUtils
{
	private static final HashMap<String, Color> colors = createColors();
	private static HashMap<String, Color> createColors()
	{
		HashMap<String, Color> hashMap = new HashMap<>();
		hashMap.put("white    ".toLowerCase().trim(), Color.white    );
		hashMap.put("lightGray".toLowerCase().trim(), Color.lightGray);
		hashMap.put("gray     ".toLowerCase().trim(), Color.gray     );
		hashMap.put("darkGray ".toLowerCase().trim(), Color.darkGray );
		hashMap.put("black    ".toLowerCase().trim(), Color.black    );
		hashMap.put("red      ".toLowerCase().trim(), Color.red      );
		hashMap.put("pink     ".toLowerCase().trim(), Color.pink     );
		hashMap.put("orange   ".toLowerCase().trim(), Color.orange   );
		hashMap.put("yellow   ".toLowerCase().trim(), Color.yellow   );
		hashMap.put("green    ".toLowerCase().trim(), Color.green    );
		hashMap.put("magenta  ".toLowerCase().trim(), Color.magenta  );
		hashMap.put("cyan     ".toLowerCase().trim(), Color.cyan     );
		hashMap.put("blue     ".toLowerCase().trim(), Color.blue     );
		return hashMap;
	}
	
	static String serializeColor(Color color)
	{
		for (Entry<String, Color> entry : colors.entrySet())
		{
			if (color.equals(entry.getValue()))
			{
				return entry.getKey();
			}
		}
		return color.getRed() + "." + color.getGreen() + "." + color.getBlue();
	}
	
	static Color getColor(String serialized)
	{
		if (serialized == null)
		{
			return null;
		}
		Color c = colors.get(serialized.toLowerCase().trim());
		if (c != null)
		{
			return c;
		}
		String[] values = serialized.split("\\.");
		if (values.length != 3)
		{
			return null;
		}
		try
		{
			return new Color(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]));
		}
		catch (NumberFormatException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to parser color: " + serialized, e);
			return null;
		}
	}

	static boolean descend(Container container)
	{
		return true;
	}

	static boolean shouldAddPopups(Container container)
	{
//		if (container instanceof javax.swing.CellRendererPane)
//		{
//			return false;
//		}
//		if (container instanceof JPopupMenu)
//		{
//			return false;
//		}
//		if (container instanceof JMenuItem)
//		{
//			return false;
//		}
		return true;
	}

	static String getBackgroundKey(String listenerKey)
	{
		return listenerKey + ":background";
	}

	static String getForegroundKey(String listenerKey)
	{
		return listenerKey + ":foreground";
	}
}
