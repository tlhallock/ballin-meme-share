
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



package org.cnv.shr.gui.tbl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;

// Still need to make a serve table...


public abstract class DbJTable<T> extends MouseAdapter
{
	// Still have to make paths table and files table...
	
	private JPopupMenu jPopupMenu;
	private TableRightClickListener dblClick;
	private String keyName;
	private String[] names;
	protected JTable table;
	
	
	public DbJTable(JTable table, String keyName)
	{
		this.table = table;
		jPopupMenu = new JPopupMenu();
		table.setComponentPopupMenu(jPopupMenu);
		table.addMouseListener(this);
		table.setAutoCreateRowSorter(true);
		this.keyName = keyName;
		setColumnNames();
	}
	
	private void setColumnNames()
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int columnCount = model.getColumnCount();
		names = new String[columnCount];
		for (int i = 0; i < columnCount; i++)
		{
			names[i] = model.getColumnName(i);
		}
	}

	public void empty()
	{
		// The entire reason these classes exist is to put refreshes on the event queue
		SwingUtilities.invokeLater(() -> {
			synchronized (table)
			{
				emptyInternal();
			}
		});
	}

	private synchronized void emptyInternal()
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}
	}

	public void refreshInPlace()
	{
		// The entire reason these classes exist is to put refreshes on the event queue
		SwingUtilities.invokeLater(() -> {
				refreshInPlaceInternal();
		});
	}
	
	private synchronized void refreshInPlaceInternal()
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int columnCount = model.getColumnCount();
		HashMap<String, Object> values = new HashMap<>();
		
		int rowCount = table.getRowCount();
		for (int row = 0; row < rowCount; row++)
		{
			if (!fillRow(create(row), values))
			{
				refresh();
				return;
			}
			for (int col = 0; col < columnCount; col++)
			{
				model.setValueAt(values.get(names[col]), row, col);
			}
		}
	}

	public void refresh()
	{
		// The entire reason these classes exist is to put refreshes on the event queue

		SwingUtilities.invokeLater(() -> {
			synchronized (table)
			{
				refreshInternal();
			}
		});
	}
	
	private synchronized void refreshInternal()
	{
		emptyInternal();
		
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int columnCount = model.getColumnCount();
		HashMap<String, Object> currentRow = new HashMap<>();
		Object[] rowData = new Object[columnCount];
		
		try (CloseableIterator<T> it = list();)
		{
			while (it.hasNext())
			{
				T t = it.next();
				try
				{
					if (!fillRow(t, currentRow))
					{
						continue;
					}
				}
				catch (Exception ex)
				{
					LogWrapper.getLogger().log(Level.INFO, null, ex);
					continue;
				}
				for (int i = 0; i < columnCount; i++)
				{
					rowData[i] = currentRow.get(names[i]);
				}
				model.addRow(rowData);
			}
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list:", e);
		}
	}
	
	public void refresh(T t)
	{
		SwingUtilities.invokeLater(() -> {
				synchronized (table)
				{
					refreshInternal(t);
				}
		});
	}
	

	private synchronized void refreshInternal(T t)
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		
		int columnCount = model.getColumnCount();
		int keyIndex = -1;
		for (int i = 0; i < columnCount; i++)
		{
			if (names[i].equals(keyName))
			{
				keyIndex = i;
			}
		}
		HashMap<String, Object> values = new HashMap<>();
		if (!fillRow(t, values))
		{
			return;
		}
			
		String needle = (String) values.get(keyName);
		
		int rowCount = table.getRowCount();
		for (int row = 0; row < rowCount; row++)
		{
			String cValue = (String) model.getValueAt(row, keyIndex);
			if (!cValue.equals(needle))
			{
				continue;
			}
			
			for (int col = 0; col < columnCount; col++)
			{
				model.setValueAt(values.get(names[col]), row, col);
			}
			
			return;
		}

		Object[] rowData = new Object[columnCount];
		for (int i = 0; i < columnCount; i++)
		{
			rowData[i] = values.get(names[i]);
		}
		model.addRow(rowData);
	}
	
	public void setValues(T t, HashMap<String, Object> vals, int rowGuess)
	{
		SwingUtilities.invokeLater(() -> {
			synchronized (table)
			{
				setValuesInternal(t, vals, rowGuess);
			}
		});
	}
	private synchronized void setValuesInternal(T t, HashMap<String, Object> vals, int rowGuess)
	{
		if (rowGuess < 0 || rowGuess > table.getRowCount())
		{
			return;
		}
		Object valueAt = table.getValueAt(rowGuess, table.getColumn(keyName).getModelIndex());
		Object expectedValue = vals.get(keyName);
		
		if (!valueAt.equals(expectedValue))
		{
			// Fix the rowGuess changing problem...
			return;
		}
		for (Entry<String, Object> entry : vals.entrySet())
		{
			table.setValueAt(entry.getValue(), rowGuess, 
					table.getColumn(entry.getKey()).getModelIndex());
		}
	}

	protected abstract T create(HashMap<String, Object> currentRow);
	protected abstract boolean fillRow(T t, HashMap<String, Object> currentRow);
	protected abstract CloseableIterator<T> list();
	
	protected void setDblClick(TableRightClickListener listener)
	{
		this.dblClick = listener;
	}
	
	private T create(int row)
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		HashMap<String, Object> rowData = new HashMap<>();
		for (int i=0; i<table.getColumnCount(); i++)
		{
			rowData.put(model.getColumnName(i), model.getValueAt(row,  i));
		}
		return create(rowData);
	}

	protected void addListener(TableRightClickListener l)
	{
		addListener(l, false);
	}
	protected void addListener(TableRightClickListener l, boolean mkDblClick)
	{
		JMenuItem menu = new JMenuItem(l.getName());
		menu.addActionListener(l);
		if (mkDblClick)
		{
			LogWrapper.getLogger().info("Default action for " + getClass().getName() + " is " + l.getName());
			dblClick = l;
		}
		jPopupMenu.add(menu);
	}
	
//	public void removeSelected()
//	{
//		// The entire reason these classes exist is to put refreshes on the event queue
//		SwingUtilities.invokeLater(() -> {
//			synchronized (table)
//			{
//				removeSelectedInternal();
//			}
//		});
//	}
	
	private int[] getSelectedRows()
	{
		int[] selectedRows2 = table.getSelectedRows();
		for (int i=0;i<selectedRows2.length;i++)
		{
			selectedRows2[i] = table.convertRowIndexToModel(selectedRows2[i]);
		}
		Arrays.sort(selectedRows2);
		return selectedRows2;
	}

	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	private void checkPopup(MouseEvent me)
	{
		if (!me.isPopupTrigger())
		{
			return;
		}
//		table.rowAtPoint(me.getPoint());
		jPopupMenu.show(me.getComponent(), me.getX(), me.getY());
		jPopupMenu.setVisible(true);
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		checkPopup(e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		checkPopup(e);
	}
	
	@Override
	public synchronized void mouseClicked(MouseEvent me)
	{
		int nRow = table.convertRowIndexToModel(table.rowAtPoint(me.getPoint()));
//		if (isSingleSelection())
//		{
//			// ensures row highlighted, even for right clicks...
//			if (nRow >= 0 && nRow < table.getRowCount())
//			{
//				table.setRowSelectionInterval(nRow, nRow);
//			}
//			else
//			{
//				table.clearSelection();
//			}
//		}
		
		if (me.getClickCount() >= 2 && dblClick != null)
		{
			int[] selectedRows = new int[] { nRow };
			for (int i = selectedRows.length - 1; i >= 0; i--)
			{
				dblClick.perform(create(selectedRows[i]));
			}
		}
		else
		{
			checkPopup(me);
		}
	}
	
	
	
	
	
	
	
	
	
	abstract class TableRightClickListener implements ActionListener
	{
		abstract String getName();
		abstract void perform(T t);

		@Override
		public void actionPerformed(ActionEvent e)
		{
			SwingUtilities.invokeLater(() -> {
				int[] selectedRows2 = getSelectedRows();
				if (selectedRows2 == null)
				{
					LogWrapper.getLogger().info("No rows selected!");
					return;
				}
				for (int i = selectedRows2.length - 1; i >= 0; i--)
				{
					final T create = create(selectedRows2[i]);
					if (create == null)
					{
						LogWrapper.getLogger().info("Unable to find record from " + selectedRows2[i]);
						return;
					}

					LogWrapper.getLogger().info("Performing action " + getName() + " from " + getClass().getName());
					Services.userThreads.execute(() -> {
						try
						{
							perform(create);
						}
						catch (Exception ex)
						{
							LogWrapper.getLogger().log(Level.INFO, null, ex);
						}
					});
				}
			});
		}
	}
}

