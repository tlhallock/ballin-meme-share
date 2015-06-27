package org.cnv.shr.gui.color;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

class ColorSplitPaneUI extends BasicSplitPaneUI
{
	Color myBackground;
	
	ColorSplitPaneUI(Color color)
	{
		myBackground = color;
	}

	@Override
	public BasicSplitPaneDivider createDefaultDivider()
	{
		return new BasicSplitPaneDivider(this) {
			@Override
			public void paint(Graphics g)
			{
				g.setColor(myBackground);
				g.fillRect(0, 0, getSize().width, getSize().height);
				super.paint(g);
			}
		};
	}
}
