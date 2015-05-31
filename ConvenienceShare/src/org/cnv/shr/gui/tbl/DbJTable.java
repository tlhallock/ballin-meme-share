package org.cnv.shr.gui.tbl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbObject;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.util.LogWrapper;

public abstract class DbJTable<T extends DbObject<? extends Number>> extends MouseAdapter
{
	// Still have to make paths table and files table...
	
	private JPopupMenu jPopupMenu;
	private TableRightClickListener dblClick;
	private int row = -1;
	private JTable table;
	
	public DbJTable(JTable table)
	{
		this.table = table;
		jPopupMenu = new JPopupMenu();
		table.addMouseListener(this);
		table.setAutoCreateRowSorter(true);
	}

	public synchronized void refresh(T t)
	{
	}

	public void refresh()
	{
		// The entire reason these classes exist is to put refreshes on a different thread
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run()
			{
				refreshInternal();
			}});
	}
	
	private synchronized void refreshInternal()
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}

		try (MyIt<T> it = list();)
		{
			int columnCount = table.getColumnCount();
			String[] names = new String[columnCount];
			for (int i = 0; i < columnCount; i++)
			{
				names[i] = table.getColumnName(i);
			}

			Object[] rowData = new String[columnCount];
			HashMap<String, Object> currentRow = new HashMap<>();
			while (it.hasNext())
			{
				T t = it.next();
				fillRow(t, currentRow);
				for (int i = 0; i < columnCount; i++)
				{
					rowData[i] = currentRow.get(names[i]);
				}
				model.addRow(rowData);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void setValues(T t, HashMap<String, Object> vals, int rowGuess)
	{
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run()
			{
				setValuesInternal(t, vals, rowGuess);
			}});
	}
	private synchronized void setValuesInternal(T t, HashMap<String, Object> vals, int rowGuess)
	{
		// Fix the rowGuess changing problem.....
		
		// Also, get the column by the name:
		for (Entry<String, Object> entry : vals.entrySet())
		{
			// Should do stuff here...
			table.getColumn(entry.getKey());
		}

		int columnCount = table.getColumnCount();
		for (int i=0;i<columnCount;i++)
		{
			Object updatedValue = vals.get(table.getColumnName(i));
			if (updatedValue == null)
			{
				continue;
			}
			table.setValueAt(updatedValue, rowGuess, i);
		}
	}

	protected abstract T create(HashMap<String, Object> currentRow);
	protected abstract void fillRow(T t, HashMap<String, Object> currentRow);
	protected abstract MyIt<T> list();
	
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
			rowData.put(model.getColumnName(i), table.getValueAt(row, i));
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
			dblClick = l;
		}
		jPopupMenu.add(menu);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void checkPopup(MouseEvent me)
	{
		if (!me.isPopupTrigger())
		{
			return;
		}
		row = table.rowAtPoint(me.getPoint());
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
		int nRow = table.rowAtPoint(me.getPoint());
		if (nRow >= 0 && nRow < table.getRowCount())
		{
			table.setRowSelectionInterval(nRow, nRow);
		}
		else
		{
			table.clearSelection();
		}
		
		if (me.getClickCount() >= 2 && dblClick != null)
		{
			dblClick.perform(create(row));
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
			final T create = create(row);
			if (create == null)
			{
				LogWrapper.getLogger().info("Unable to find record from " + row);
				return;
			}

			Services.userThreads.execute(new Runnable() {
				public void run() {
					perform(create);
				};
			});
		}
	}
	
	
	public interface MyIt<T extends DbObject> extends Iterator<T>, Closeable {}
}
