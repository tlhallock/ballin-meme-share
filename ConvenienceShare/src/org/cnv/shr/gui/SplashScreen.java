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
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SplashScreen extends JFrame
{
	public static SplashScreen showSplash()
	{
		Timer timer = new Timer();
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
		screen.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e)
			{
				timer.cancel();
			}
		});

		LinkedList<Circle> circles = new LinkedList<>();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run()
			{
				circles.add(new Circle(screen.getContentPane().getWidth(), screen.getContentPane().getHeight()));
				screen.repaint();
			}}, 1000, 500);

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

				Graphics2D graphics2D = (Graphics2D) g;
				AttributedString attributedString = new AttributedString("ConvenienceShare is starting...");
				attributedString.addAttribute(TextAttribute.FOREGROUND, Color.white);
		    AttributedCharacterIterator characterIterator = attributedString.getIterator();
		    FontRenderContext fontRenderContext = graphics2D.getFontRenderContext();
		    LineBreakMeasurer lbm = new LineBreakMeasurer(characterIterator, fontRenderContext);
		    TextLayout textLayout = lbm.nextLayout(Integer.MAX_VALUE);
		    double width = textLayout.getBounds().getWidth();
		    double height = textLayout.getBounds().getHeight();
				
				textLayout.draw(graphics2D, (float) (getWidth() - width) / 2, (float) (getHeight() - height) / 2 );
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
