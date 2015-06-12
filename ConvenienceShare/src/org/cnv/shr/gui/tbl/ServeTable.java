package org.cnv.shr.gui.tbl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.ServeInstance;
import org.cnv.shr.util.CloseableIterator;

public class ServeTable extends DbJTable<ServeInstance>
{
	public ServeTable(JTable table)
	{
		super(table, "probably something like the ip");
		
		// add listeners...
	}

	@Override
	protected CloseableIterator<ServeInstance> list()
	{
		Iterator<ServeInstance> iterator = Services.server.getServeInstances().iterator();
		return new CloseableIterator<ServeInstance>()
		{
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public ServeInstance next()
			{
				return iterator.next();
			}

			@Override
			public void close() throws IOException {}
		};
	}

	@Override
	protected ServeInstance create(HashMap<String, Object> currentRow)
	{
		for (ServeInstance instance : Services.server.getServeInstances())
		{
			if (instance.getMachine().getName().equals(currentRow.get("Machine"))
						&& instance.getFile().getPath().getFullPath().equals(currentRow.get("File")))
			{
				return instance;
			}
		}
		return null;
	}

	@Override
	protected void fillRow(ServeInstance t, HashMap<String, Object> currentRow)
	{
		currentRow.put("Machine", t.getMachine().getName());                    
		currentRow.put("File",    t.getFile().getPath().getFullPath());     
		currentRow.put("Percent", String.valueOf(t.getCompletionPercentage())); 
		currentRow.put("Speed",   "Speed goes here...");                                
	}

	public static DefaultTableModel createTableModel()
	{
		return new javax.swing.table.DefaultTableModel(new Object[][] {

		}, new String[] { "Machine", "File", "Percent", "Speed" })
		{
			Class[] types = new Class[] { java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class };

			public Class getColumnClass(int columnIndex)
			{
				return types[columnIndex];
			}
		};
	}
}
