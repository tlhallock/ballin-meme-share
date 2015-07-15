
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

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbChunks;
import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.dmn.dwn.DownloadManager.GuiInfo;
import org.cnv.shr.gui.Application;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DownloadTable extends DbJTable<Download>
{
	private Application app;
	
	public DownloadTable(Application app, JTable table)
	{
		super(app, table, "Id");
		this.app = app;

		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				for (Download download : downloads)
				{
					DownloadState state = download.getState();
					if (state == null)
					{
						// why would this happen?
						LogWrapper.getLogger().warning("The download has no state.");
						return;
					}
					if (!state.equals(DownloadState.ALL_DONE))
					{
	
						JOptionPane.showMessageDialog(app, 
								"Opening unfinished downloads is not supported yet.",
								"This download is not done!",
								 JOptionPane.ERROR_MESSAGE);
						return;
					}
					Misc.nativeOpen(download.getTargetFile(), false);
				}
			}
			
			@Override
			String getName()
			{
				return "Open";
			}
		}, true);
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				for (Download download : downloads)
				{
					DownloadState state = download.getState();
					if (state == null)
					{
						// why would this happen?
						LogWrapper.getLogger().warning("The download has no state.");
						return;
					}
					if (state.hasYetTo(DownloadState.ALLOCATING))
					{
						JOptionPane.showMessageDialog(app, 
								"Opening queued downloads is not supported yet.",
								"This download is not done!",
								 JOptionPane.ERROR_MESSAGE);
						return;
					}
					Misc.nativeOpen(download.getTargetFile(), true);
				}
			}
			
			@Override
			String getName()
			{
				return "Open containing folder";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				for (Download download : downloads)
				{
					DownloadInstance dInstance = Services.downloads.getDownloadInstanceForGui(
							download.getFile().getFileEntry());
					if (dInstance != null)
					{
						dInstance.fail("User quit.");
					}
					download.delete();
				}
				
				//Services.notifications.downloadDeleted(download);
				refresh();
				
			}
			
			@Override
			String getName()
			{
				return "Delete";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				for (Download download : downloads)
				{
					DownloadInstance.testCompletion(download);
				}
			}
			
			@Override
			String getName()
			{
				return "Verify Integrity";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				for (Download download : downloads)
				{
					DbChunks.removeAllChunks(download);
					download.setState(DownloadState.REQUESTING_METADATA);
				}
			}
			
			@Override
			String getName()
			{
				return "Update Metadata";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				SpinnerNumberModel sModel = new SpinnerNumberModel();
				JSpinner spinner = new JSpinner(sModel);
				if (JOptionPane.OK_OPTION != JOptionPane.showOptionDialog(
						app,
						spinner,
						"New priorities",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null, null, null))
				{
					return;
				}
				int priority = ((Number) sModel.getValue()).intValue();
				for (Download download : downloads)
				{
					download.setPriority(priority);
					download.tryToSave();
				}
			}
			
			@Override
			String getName()
			{
				return "Set Priority";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download[] downloads)
			{
				for (Download download : downloads)
				{
					download.setState(DownloadState.QUEUED);
					download.tryToSave();
				}
			}
			
			@Override
			String getName()
			{
				return "Restart (fix this)";
			}
		});
		
		table.setEnabled(true);
	}

	protected void delete(Download t) { t.delete(); }
	protected boolean isSingleSelection()
	{
		return false;
	}

	@Override
	protected Download create(HashMap<String, Object> currentRow)
	{
		return DbDownloads.getDownload(Integer.parseInt((String) currentRow.get("Id")));
	}

	@Override
	protected boolean fillRow(Download download, HashMap<String, Object> currentRow)
	{
		SharedFile file;
		RootDirectory directory;
		Machine machine;
		FileEntry fileEntry;
		
		if (download == null)
		{
			return false;
		}
		
		try
		{
			file = download.getFile();
			directory = file.getRootDirectory();
			machine = directory.getMachine();

			fileEntry = file.getFileEntry();
		}
		catch (Exception ex)
		{
			DbDownloads.cleanUnchecksummedFiles();
			LogWrapper.getLogger().log(Level.INFO, "Found bad download.", ex);
			currentRow.clear();
			return false;
		}
		
		GuiInfo guiInfo = Services.downloads.getGuiInfo(download);
		
		currentRow.put("Machine",           machine.getName()                                                                     );
		currentRow.put("Directory",         directory.getName()                                                                   );
		currentRow.put("File",              file.getPath().getUnbrokenName()                                                      );
		currentRow.put("Size",              new DiskUsage(file.getFileSize())                                                     );
		currentRow.put("Added on",          new Date(download.getAdded())                                                         );
		currentRow.put("Status",            download.getState().humanReadable()                                                   );
		currentRow.put("Priority",          String.valueOf(download.getPriority())                                                );
		currentRow.put("Local path",        download.getTargetFile().toString()                                                   );
		currentRow.put("Number of Mirrors", guiInfo.numSeeders                                                                    );
		currentRow.put("Speed",             guiInfo.speed                                                                         );
		currentRow.put("Percent",           download.getState().equals(DownloadState.ALL_DONE) ?  "100" : guiInfo.percentComplete );                     
		currentRow.put("Id",                String.valueOf(download.getId())                                                      );
		
		return true;
	}

	@Override
	protected CloseableIterator<Download> list()
	{
		return DbDownloads.listDownloads();
	}

	public static DefaultTableModel createTableModel()
	{
    return new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Machine", "Directory", "File", "Size", "Added on", "Status", "Priority", "Local path", "Number of Mirrors", "Speed", "Percent", "Id"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, DiskUsage.class, java.util.Date.class, java.lang.Object.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, 
            // speed
            java.lang.Object.class, 
            //percent
            java.lang.Object.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false, false, false, false, false, false, false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    };
	}

	@Override
	protected Download[] createArray(int length)
	{
		return new Download[length];
	}
}
