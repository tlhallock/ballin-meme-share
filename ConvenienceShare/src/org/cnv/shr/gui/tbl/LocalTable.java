
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

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.Application;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.gui.LocalDirectoryView;
import org.cnv.shr.gui.NumberOfFiles;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class LocalTable extends DbJTable<LocalDirectory>
{
	private Application app;

	public LocalTable(Application app, JTable table)
	{
		super(table, "Path");
		this.app = app;

		addListener(new TableRightClickListener()
		{
			@Override
			String getName()
			{
				return "Show";
			}
			@Override
			void perform(LocalDirectory root)
			{
						LocalDirectoryView localDirectoryView = new LocalDirectoryView(root);
						Services.notifications.registerWindow(localDirectoryView);
						localDirectoryView.setVisible(true);
						LogWrapper.getLogger().info("Displaying " + root.getName());
			}
		}, true);
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(LocalDirectory root)
			{
				UserActions.remove(root);
			}

			@Override
			public String getName()
			{
				return "Delete";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(LocalDirectory root)
			{
				Misc.nativeOpen(Paths.get(root.getPathElement().getFsPath()), false);
			}

			@Override
			public String getName()
			{
				return "Open";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(LocalDirectory root)
			{
				UserActions.userSync(root, Collections.singletonList(app.createLocalListener(root)));
			}
			@Override
			public String getName()
			{
				return "Synchronize";
			}
		});
	}

	@Override
	protected LocalDirectory create(HashMap<String, Object> currentRow)
	{
		return DbRoots.getLocal((String) currentRow.get("Path"));
	}

	@Override
	protected void fillRow(LocalDirectory local, HashMap<String, Object> currentRow)
	{
    currentRow.put("Path"           , local.getPathElement().getFullPath()   );
    currentRow.put("Name"           , local.getName()                        );
    currentRow.put("Description"    , local.getDescription()                 );
    currentRow.put("Tags"           , local.getTags()                        );
    currentRow.put("Number of files", new NumberOfFiles(local.numFiles())    );
    currentRow.put("Total file size", new DiskUsage(local.diskSpace())       );
	}                                                                               

	@Override
	protected CloseableIterator<LocalDirectory> list()
	{
		return DbRoots.listLocals();
	}

	public static DefaultTableModel createTableModel()
	{
		return new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Path", "Name", "Description", "Tags", "Number of files", "Total file size"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, NumberOfFiles.class, DiskUsage.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    };
	}
}
