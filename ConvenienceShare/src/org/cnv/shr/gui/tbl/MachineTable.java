package org.cnv.shr.gui.tbl;

import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.Application;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.gui.NumberOfFiles;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;

public class MachineTable extends DbJTable<Machine>
{
	private Application app;

	public MachineTable(Application app, JTable table)
	{
		super(table);
		this.app = app;
		
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Machine t)
			{
				app.showRemote(t);
			}
			
			@Override
			String getName()
			{
				return "Show";
			}
		}, true);
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Machine t)
			{
				UserActions.syncRoots(t);
			}
			
			@Override
			String getName()
			{
				return "Synchronize roots";
			}
		});
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Machine t)
			{
				if (t.isLocal())
				{
					JOptionPane.showMessageDialog(app, 
							"Unable to delete the local machine.",
							"Unable to Delete.",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				UserActions.removeMachine(t);
			}
			
			@Override
			String getName()
			{
				return "Delete";
			}
		});
	}

	@Override
	protected Machine create(HashMap<String, Object> currentRow)
	{
		return DbMachines.getMachine((String) currentRow.get("Id"));
	}

	@Override
	protected void fillRow(Machine machine, HashMap<String, Object> currentRow)
	{
		if (machine.isLocal())
		{
			currentRow.put("Name",              "Local machine: " + Services.localMachine.getName()                  );
			currentRow.put("Current Address",   Services.localMachine.getIp() + ":" + Services.localMachine.getPort());
			currentRow.put("Id",                Services.localMachine.getIdentifier()                                );
			currentRow.put("Sharing",           String.valueOf(Services.localMachine.sharingWithOther())             );
			currentRow.put("Number of files",   new NumberOfFiles(DbMachines.getTotalNumFiles(Services.localMachine)));
			currentRow.put("Total files size",  new DiskUsage(DbMachines.getTotalDiskspace(Services.localMachine))   );
			currentRow.put("Last Ip",           Services.settings.getLocalIp()                                       );
			currentRow.put("Port",              Services.settings.servePortBeginE.get()                              );
			currentRow.put("Number of ports",   Services.settings.maxServes.get()                                    );
		}
		else
		{
			currentRow.put("Name",              machine.getName()                                      );
  		currentRow.put("Current Address",   machine.getIp() + ":" + machine.getPort()              );
  		currentRow.put("Id",                machine.getIdentifier()                                );
      currentRow.put("Sharing",           String.valueOf(machine.sharingWithOther())             );
      currentRow.put("Number of files",   new NumberOfFiles(DbMachines.getTotalNumFiles(machine)));
      currentRow.put("Total files size",  new DiskUsage(DbMachines.getTotalDiskspace(machine))   );
      currentRow.put("Last Ip",           machine.getIp()                                        );
      currentRow.put("Port",              machine.getPort()                                      );
      currentRow.put("Number of ports",   machine.getNumberOfPorts()                             );
		}
	}

	@Override
	protected org.cnv.shr.gui.tbl.DbJTable.MyIt<Machine> list()
	{
		return DbMachines.listMachines();
	}
}
