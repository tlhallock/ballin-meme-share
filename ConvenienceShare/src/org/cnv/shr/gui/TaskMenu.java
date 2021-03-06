
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */



package org.cnv.shr.gui;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Icon;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.Misc;

/**
 *
 * @author thallock
 */
public class TaskMenu extends javax.swing.JFrame {

	private int initialClickX;
	private int initialClickY;
	
        
        
    /**
     * Creates new form TaskMenu
     */
    public TaskMenu(Icon icon, final PopupMenu menu) {
        initComponents();
        jLabel1.setIcon(icon);
        add(menu);
        jLabel1.addMouseListener(new MouseAdapter() {
            private void doPopup(MouseEvent me) {
                if (me.isPopupTrigger()) {
                    menu.show(me.getComponent(), me.getX(), me.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent me) {
                doPopup(me);
                if (me.getClickCount() >= 2)
                {
                	UserActions.showGui();
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                doPopup(me);
                initialClickX = me.getX();
                initialClickY = me.getY();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                doPopup(me);
            }
        });

        jLabel1.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                setLocation(getLocation().x + e.getX() - initialClickX,
                		    getLocation().y + e.getY() - initialClickY);
            }
        });
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        int maxX = -1;
        int maxY = -1;

        // go to the bottom right...
        int FUDGE = 10;
        for (GraphicsDevice device : env.getScreenDevices())
        {
        	Rectangle bounds = device.getDefaultConfiguration().getBounds();
        	int x = bounds.x + bounds.width - getWidth() - FUDGE;
        	int y = bounds.y + bounds.height - getHeight() - FUDGE;
        	if (x < maxX)
        	{
        		continue;
        	}
        	if (y < maxY || x > maxX)
        	{
        		maxX = x;
        		maxY = y;
        	}
        }
        setLocation(maxX, maxY);
        
        this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(WindowEvent e)
			{
				Services.quiter.quit();
			}
		});
    }
    
    private Dimension getIconSize()
    {
        return new Dimension(Misc.ICON_SIZE, Misc.ICON_SIZE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(getIconSize());
        setMinimumSize(getIconSize());
        setUndecorated(true);
        setPreferredSize(getIconSize());
        setResizable(false);

        jLabel1.setMinimumSize(new java.awt.Dimension(22, 22));
        jLabel1.setName(""); // NOI18N
        jLabel1.setPreferredSize(new java.awt.Dimension(22, 22));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
