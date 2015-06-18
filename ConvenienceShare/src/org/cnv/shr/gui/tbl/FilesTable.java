
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.Misc;

public class FilesTable extends DbJTable<SharedFile>
{
	private LinkedList<SharedFile> currentlyDisplaying = new LinkedList<>();
	private String currentRootName;
	private String currentMachineIdent;
	private String filter;
	
	public FilesTable(JTable table, final JFrame origin)
	{
		super(table, "I don't think this is used yet.");

		addListener(new TableRightClickListener()
		{
			@Override
			void perform(SharedFile t)
			{
        UserActions.download(t);
			}

			@Override
			String getName()
			{
				return "Download";
			}
		}, true);
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(SharedFile t)
			{
				if (t.isLocal())
				{
					Misc.nativeOpen(((LocalFile) t).getFsFile(), false);
				}
				else
				{
					JOptionPane.showMessageDialog(origin,
							"Unable to open remote file, download it first.",
							"Unable to open remote file.",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			@Override
			String getName()
			{
				return "Open";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(SharedFile t)
			{
				if (t.isLocal())
				{
					Misc.nativeOpen(((LocalFile) t).getFsFile(), true);
				}
				else
				{
					JOptionPane.showMessageDialog(origin,
							"Unable to open remote file, download it first.",
							"Unable to open remote file.",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			@Override
			String getName()
			{
				return "Show in folder";
			}
		});
	}

	public void setCurrentlyDisplaying(String currentMachineIdent, String currentRoot, List<SharedFile> list)
	{
		this.currentMachineIdent = currentMachineIdent;
		this.currentRootName = currentRoot;
		synchronized (currentlyDisplaying)
		{
			this.currentlyDisplaying.clear();
			this.currentlyDisplaying.addAll(list);
		}
		refresh();
	}
	
	@Override
	protected SharedFile create(HashMap<String, Object> currentRow)
	{
    final String dirname = (String) currentRow.get("Path");
    final String basename = (String) currentRow.get("Name");
    final String fullPath = dirname + basename;
    RootDirectory directory = getRootDirectory();
    return DbFiles.getFile(directory, DbPaths.getPathElement(fullPath));
	}

	private RootDirectory getRootDirectory()
	{
		return DbRoots.getRoot(DbMachines.getMachine(currentMachineIdent), currentRootName);
	}

	@Override
	protected void fillRow(SharedFile next, HashMap<String, Object> currentRow)
	{
    final String path = next.getPath().getFullPath();

    final int indexSlh = path.lastIndexOf('/');
    final String name = indexSlh < 0 ? path : path.substring(indexSlh + 1);
    final String relPath = indexSlh < 0 ? "" : path.substring(0, indexSlh + 1);

    final int indexExt = name.lastIndexOf('.');
    final String ext = indexExt < 0 ? "" : name.substring(indexExt);
    
		 currentRow.put("Path",           String.valueOf(relPath)             );
		 currentRow.put("Name",           String.valueOf(name)                );
		 currentRow.put("Size",           new DiskUsage(next.getFileSize())   );
		 currentRow.put("Checksum",       String.valueOf(next.getChecksum())  );
		 currentRow.put("Description",    String.valueOf(next.getTags())      );
		 currentRow.put("Modified",       new Date(next.getLastUpdated())     );
		 currentRow.put("Extension",      String.valueOf(ext)                 );
	}

	@Override
	protected CloseableIterator<SharedFile> list()
	{
		return new CloseableIterator<SharedFile>()
		{
			// Not synchronized on currently displaying...
			Iterator<SharedFile> delegate = currentlyDisplaying.iterator();
			
			@Override
			public boolean hasNext()
			{
				return delegate.hasNext();
			}

			@Override
			public SharedFile next()
			{
				return delegate.next();
			}

			@Override
			public void close() throws IOException {}
		};
	}

	public static DefaultTableModel createTableModel()
	{
		return new javax.swing.table.DefaultTableModel(
      new Object [][] {

      },
      new String [] {
          "Path", "Name", "Size", "Checksum", "Description", "Modified", "Extension"
      }
  ) {
      Class[] types = new Class [] {
          java.lang.String.class, java.lang.String.class, DiskUsage.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class
      };
      boolean[] canEdit = new boolean [] {
          false, false, false, false, false, false, false
      };

      @Override
			public Class getColumnClass(int columnIndex) {
          return types [columnIndex];
      }

      @Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
          return canEdit [columnIndex];
      }
  	};
	}
}
