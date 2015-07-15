package org.cnv.shr.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.LinkedList;
import java.util.Random;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class SplashScreen extends JFrame
{
	private String additionalText = "";
	public void setStatus(String text)
	{
		additionalText = text;
		if (additionalText == null)
		{
			additionalText = "";
		}
	}
	
	public static SplashScreen showSplash()
	{
		TimerTask startUpMonitor = new TimerTask() { public void run() {
				LogWrapper.getLogger().info("Unable to start main application within one minute!!!!");
				System.exit(-1);
			}};
		Misc.timer.schedule(startUpMonitor, 60 * 1000);
		SplashScreen screen = new SplashScreen();

    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    int splashWidth  = screenBounds.width  / 4;
    int splashHeight = screenBounds.height / 4;
		screen.setBounds(new Rectangle(
				screenBounds.x + (screenBounds.width  - splashWidth ) / 2,
				screenBounds.y + (screenBounds.height - splashHeight) / 2,
				splashWidth,
				splashHeight));
		screen.setUndecorated(true);

		LinkedList<Circle> circles = new LinkedList<>();
		TimerTask prettyTask = new TimerTask() {
			@Override
			public void run()
			{
				circles.add(new Circle(screen.getContentPane().getWidth(), screen.getContentPane().getHeight()));
				screen.repaint();
			}};
		Misc.timer.scheduleAtFixedRate(prettyTask, 1000, 500);

		screen.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e)
			{
				LogWrapper.getLogger().info("Splash screen closed.");
				startUpMonitor.cancel();
			}
		});
		screen.getContentPane().setLayout(new GridLayout(0, 1));
		screen.getContentPane().add(new JPanel() {
			@Override
			public void paint(Graphics g)
			{
				g.setColor(createRandomColor());
				g.fillRect(0, 0, getWidth(), getHeight());
				
				for (Circle c : circles)
				{
					c.draw(g);
				}
				
				int textOffset = getHeight() / 2;
				textOffset += writeText((Graphics2D) g, "ConvenienceShare is starting...",  textOffset) + 10;
				textOffset += writeText((Graphics2D) g, screen.additionalText,  textOffset) + 10;
			}

			private float writeText(Graphics2D graphics2D, String text, float height)
			{
				if (text == null || text.length() == 0)
				{
					return 0;
				}
				AttributedString attributedString = new AttributedString(text);
				attributedString.addAttribute(TextAttribute.FOREGROUND, Color.white);
		    AttributedCharacterIterator characterIterator = attributedString.getIterator();
		    FontRenderContext fontRenderContext = graphics2D.getFontRenderContext();
		    LineBreakMeasurer lbm = new LineBreakMeasurer(characterIterator, fontRenderContext);
		    TextLayout textLayout = lbm.nextLayout(Integer.MAX_VALUE);
		    double textWidth = textLayout.getBounds().getWidth();
		    double textHeight = textLayout.getBounds().getHeight();
				textLayout.draw(graphics2D, (float) (getWidth() - textWidth) / 2, height - (float) textHeight / 2);
				return (float) textHeight;
			}
		});
		screen.setVisible(true);
		
		return screen;
	}

	private static final Random random = new Random();
	private static Color createRandomColor()
	{
		return new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255));
	}
	
	private static final class Circle
	{
		private final int width;
		private final int height;
		private final int x;
		private final int y;
		private final Color color;
		
		private Circle(int width, int height)
		{
			this.width = width / 4 + random.nextInt(width / 2);
			this.height = height / 4 + random.nextInt(height / 2);
			x = random.nextInt(width);
			y = random.nextInt(height);
			color = createRandomColor();
		}
		
		private void draw(Graphics g)
		{
			g.setColor(color);
			g.fillOval(x - width / 2, y - height / 2, width, height);
		}
	}
}
