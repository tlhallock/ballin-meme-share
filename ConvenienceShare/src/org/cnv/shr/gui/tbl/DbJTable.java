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
	private String keyName;
	
	public DbJTable(JTable table, String keyName)
	{
		this.table = table;
		jPopupMenu = new JPopupMenu();
		table.addMouseListener(this);
		table.setAutoCreateRowSorter(true);
		this.keyName = keyName;
	}

	public void empty()
	{
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run()
			{
				synchronized (table)
				{
					emptyInternal();
				}
			}});
	}

	private synchronized void emptyInternal()
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		while (model.getRowCount() > 0)
		{
			model.removeRow(0);
		}
	}

	public void refresh()
	{
		// The entire reason these classes exist is to put refreshes on the event queue
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run()
			{
				synchronized (table)
				{
					refreshInternal();
				}
			}});
	}
	
	private synchronized void refreshInternal()
	{
		emptyInternal();
		
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		int columnCount = table.getColumnCount();
		String[] names = new String[columnCount];
		for (int i = 0; i < columnCount; i++)
		{
			names[i] = table.getColumnName(i);
		}
		HashMap<String, Object> currentRow = new HashMap<>();
		Object[] rowData = new Object[columnCount];
		
		try (MyIt<T> it = list();)
		{
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
	public void refresh(T t)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (table)
				{
					refreshInternal(t);
				}
			}
		});
	}

	private synchronized void refreshInternal(T t)
	{
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		
		int columnCount = table.getColumnCount();
		String[] names = new String[columnCount];
		int keyIndex = -1;
		for (int i = 0; i < columnCount; i++)
		{
			names[i] = table.getColumnName(i);
			if (names[i].equals(keyName))
			{
				keyIndex = i;
			}
		}
		
		HashMap<String, Object> values = new HashMap<>();
		fillRow(t, values);
		String needle = (String) values.get(keyName);
		
		int rowCount = table.getRowCount();
		for (int row = 0; row < rowCount; row++)
		{
			String cValue = (String) table.getValueAt(row, keyIndex);
			if (!cValue.equals(needle))
			{
				continue;
			}
			
			for (int col = 0; col < columnCount; col++)
			{
				table.setValueAt(values.get(names[col]), row, col);
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run()
			{
				synchronized (table)
				{
					setValuesInternal(t, vals, rowGuess);
				}
			}});
	}
	private synchronized void setValuesInternal(T t, HashMap<String, Object> vals, int rowGuess)
	{
		if (rowGuess < 0 || rowGuess > table.getRowCount())
		{
			return;
		}
		// Fix the rowGuess changing problem.....
		for (Entry<String, Object> entry : vals.entrySet())
		{
			table.setValueAt(entry.getValue(), rowGuess, 
					table.getColumn(entry.getKey()).getModelIndex());
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
			LogWrapper.getLogger().info("Default action for " + getClass().getName() + " is " + l.getName());
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
			row = nRow;
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
	
	
	public interface MyIt<T extends DbObject> extends CloseableIt<T> {}
	public interface CloseableIt<T> extends Iterator<T>, Closeable {}
}
