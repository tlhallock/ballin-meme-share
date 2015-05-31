package org.cnv.shr.gui.tbl;

import java.util.Collections;
import java.util.HashMap;

import javax.swing.JTable;

import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.Application;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.gui.LocalDirectoryView;
import org.cnv.shr.gui.NumberOfFiles;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.LocalDirectory;
import org.cnv.shr.util.LogWrapper;

public class LocalTable extends DbJTable<LocalDirectory>
{
	private Application app;

	public LocalTable(Application app, JTable table)
	{
		super(table);
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
						LocalDirectoryView localDirectoryView = new LocalDirectoryView();
						Services.notifications.registerWindow(localDirectoryView);
						localDirectoryView.view(root);
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
    currentRow.put("Description"    , local.getDescription()                 );
    currentRow.put("Tags"           , local.getTags()                        );
    currentRow.put("Number of files", new NumberOfFiles(local.numFiles())    );
    currentRow.put("Total file size", new DiskUsage(local.diskSpace())       );
	}                                                                               

	@Override
	protected org.cnv.shr.gui.tbl.DbJTable.MyIt<LocalDirectory> list()
	{
		return DbRoots.listLocals();
	}
}