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

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalFile;
import org.cnv.shr.mdl.RootDirectory;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.util.Misc;

public class FilesTable extends DbJTable<SharedFile>
{
	private LinkedList<SharedFile> currentlyDisplaying = new LinkedList<>();
	private String currentRootName;
	private String currentMachineIdent;
	
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
					Misc.nativeOpen(((LocalFile) t).getFsFile());
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
	protected org.cnv.shr.gui.tbl.DbJTable.MyIt<SharedFile> list()
	{
		return new MyIt<SharedFile>()
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
}
