package org.cnv.shr.gui.tbl;

import java.util.Date;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.cnv.shr.db.h2.DbDownloads;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.dwn.DownloadInstance;
import org.cnv.shr.gui.Application;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.mdl.Download;
import org.cnv.shr.mdl.Download.DownloadState;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;

public class DownloadTable extends DbJTable<Download>
{
	private Application app;
	
	public DownloadTable(Application app, JTable table)
	{
		super(table, "Id");
		this.app = app;

		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download download)
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
			
			@Override
			String getName()
			{
				return "Open";
			}
		}, true);
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Download download)
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
				Misc.nativeOpen(download.getTargetFile(), true);
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
			void perform(Download download)
			{
				DownloadInstance dInstance = Services.downloads.getDownloadInstanceForGui(
						download.getFile().getFileEntry());
				if (dInstance != null)
				{
					dInstance.fail("User quit.");
				}
				download.delete();
			}
			
			@Override
			String getName()
			{
				return "Delete";
			}
		});
	}

	@Override
	protected Download create(HashMap<String, Object> currentRow)
	{
		return DbDownloads.getDownload(Integer.parseInt((String) currentRow.get("Id")));
	}

	@Override
	protected void fillRow(Download download, HashMap<String, Object> currentRow)
	{
		SharedFile file = download.getFile();
		RootDirectory directory = file.getRootDirectory();
		Machine machine = directory.getMachine();
		DownloadInstance downloadInstance = Services.downloads.getDownloadInstanceForGui(file.getFileEntry());
		
		currentRow.put("Machine",           machine.getName()                                                                                                                                             );
		currentRow.put("Directory",         directory.getName()                                                                                                                                           );
		currentRow.put("File",              file.getPath().getUnbrokenName()                                                                                                                              );
		currentRow.put("Size",              new DiskUsage(file.getFileSize())                                                                                                                             );
		currentRow.put("Added on",          new Date(download.getAdded())                                                                                                                                 );
		currentRow.put("Status",            download.getState().humanReadable()                                                                                                                           );
		currentRow.put("Priority",          String.valueOf(download.getPriority())                                                                                                                        );
		currentRow.put("Local path",        download.getTargetFile().toString()                                                                                                                           );
		currentRow.put("Number of Mirrors", "1"                                                                                                                                                           );
		currentRow.put("Speed",             downloadInstance == null ? "N/A" : downloadInstance.getSpeed()                                                                                                );
		currentRow.put("Percent",           download.getState().equals(DownloadState.ALL_DONE) ?  "100%" : downloadInstance == null ? "0.0" : String.valueOf(downloadInstance.getCompletionPercentage()));
		currentRow.put("Id",                String.valueOf(download.getId())                                                                                                                              );
	}

	@Override
	protected org.cnv.shr.gui.tbl.DbJTable.MyIt<Download> list()
	{
		return DbDownloads.listDownloads();
	}
}
