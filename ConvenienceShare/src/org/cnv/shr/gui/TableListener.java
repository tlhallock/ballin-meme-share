
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class TableListener extends MouseAdapter
{
	JPopupMenu jPopupMenu;
	JTable table;
	MyTableRowListener dblClick;
	int row = -1;

	TableListener(JTable table)
	{
		this.table = table;
		jPopupMenu = new JPopupMenu();
		table.addMouseListener(this);
	}
	
	void setDoubleClick(TableRowListener listener)
	{
		dblClick = new MyTableRowListener(listener);
	}
	
	@Override
	public void mouseClicked(MouseEvent me)
	{
		row = table.rowAtPoint(me.getPoint());
		if (row >= 0 && row < table.getRowCount())
		{
			table.setRowSelectionInterval(row, row);
		}
		else
		{
			table.clearSelection();
		}
		
		if (me.getClickCount() >= 2 && dblClick != null)
		{
			dblClick.actionPerformed(null);
		}
		if (me.getButton() != java.awt.event.MouseEvent.BUTTON3)
		{
			return;
		}
		jPopupMenu.show(me.getComponent(), me.getX(), me.getY());
		jPopupMenu.setVisible(true);
	}

	TableListener addListener(TableRowListener l)
	{
		return addListener(l, false);
	}
	TableListener addListener(TableRowListener l, boolean mkDblClick)
	{
		JMenuItem menu = new JMenuItem(l.getString());
		MyTableRowListener myTableRowListener = new MyTableRowListener(l);
		menu.addActionListener(myTableRowListener);
		if (mkDblClick)
		{
			dblClick = myTableRowListener;
		}
		jPopupMenu.add(menu);
		return this;
	}
	
	static interface TableRowListener 
	{
		abstract void run(int row);
		abstract String getString();
	}
	
	private class MyTableRowListener implements ActionListener
	{
		TableRowListener listener;
		
		public MyTableRowListener(TableRowListener listener)
		{
			this.listener = listener;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			listener.run(row);
		}
	}
	
	// synchronization issue...
	public String getTableValue(String colName, int row)
	{
		for (int i = 0; i < table.getColumnCount(); i++)
		{
			if (table.getColumnName(i).equals(colName))
			{
				return (String) table.getValueAt(row, i);
			}
		}
		return null;
	}

	// synchronized...
	public static void removeIfExists(
			DefaultTableModel model,
			String columnName,
			String value)
	{
		int column = -1;
		for (int i = 0; i < model.getColumnCount(); i++)
		{
			if (model.getColumnName(i).equals(columnName))
			{
				column = i;
				break;
			}
		}
		if (column < 0)
		{
			return;
		}

		int row = -1;
		for (int i = 0; i < model.getRowCount(); i++)
		{
			if (model.getValueAt(i, column).equals(value))
			{
				row = i;
				break;
			}
		}
		if (row < 0)
		{
			return;
		}
		model.removeRow(row);
	}
}
