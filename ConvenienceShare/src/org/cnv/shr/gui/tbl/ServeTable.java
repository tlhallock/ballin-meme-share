
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

      boolean[] canEdit = new boolean [] {
          false, false, false, false
      };

      public boolean isCellEditable(int rowIndex, int columnIndex) {
          return canEdit [columnIndex];
      }
			public Class getColumnClass(int columnIndex)
			{
				return types[columnIndex];
			}
		};
	}
}
