
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

import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbMachines.Stats;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.Application;
import org.cnv.shr.gui.DiskUsage;
import org.cnv.shr.gui.NumberOfFiles;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.util.CloseableIterator;

public class MachineTable extends DbJTable<Machine>
{
	public MachineTable(Application app, JTable table)
	{
		super(app, table, "Id");
		
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(Machine[] machines)
			{
				for (Machine machine : machines)
				{
					UserActions.show(machine);
				}
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
			void perform(Machine[] machines)
			{
				for (Machine machine : machines)
				{
					UserActions.syncRoots(app, machine);
				}
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
			void perform(Machine[] machines)
			{
				for (Machine machine : machines)
				{
					if (machine.isLocal())
					{
						JOptionPane.showMessageDialog(
								app, 
								"Unable to delete the local machine.", 
								"Unable to Delete.", 
								JOptionPane.ERROR_MESSAGE);
						continue;
					}
					UserActions.removeMachine(machine);
				}
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
	protected boolean fillRow(Machine machine, HashMap<String, Object> currentRow)
	{
		Stats stats = DbMachines.getCachedStats(machine);
		if (machine.isLocal())
		{
			currentRow.put("Name",              "Local machine: " + Services.localMachine.getName()                  );
			currentRow.put("Current Address",   Services.localMachine.getIp() + ":" + Services.localMachine.getPort());
			currentRow.put("Id",                Services.localMachine.getIdentifier()                                );
			currentRow.put("Sharing",           String.valueOf(Services.localMachine.sharingWithOther())             );
			currentRow.put("Number of files",   new NumberOfFiles(stats.numberOfFiles)                               );
			currentRow.put("Total files size",  new DiskUsage(stats.totalDiskSpace)                                  );
			currentRow.put("Last Ip",           Services.settings.getLocalIp()                                       );
			currentRow.put("Port",              Services.settings.servePortBeginE.get()                              );
			currentRow.put("Number of ports",   Services.settings.numHandlers.get()                                  );
		}
		else
		{
			currentRow.put("Name",              machine.getName()                                      );
  		currentRow.put("Current Address",   machine.getIp() + ":" + machine.getPort()              );
  		currentRow.put("Id",                machine.getIdentifier()                                );
      currentRow.put("Sharing",           String.valueOf(machine.sharingWithOther())             );
			currentRow.put("Number of files",   new NumberOfFiles(stats.numberOfFiles)                 );
			currentRow.put("Total files size",  new DiskUsage(stats.totalDiskSpace)                    );
      currentRow.put("Last Ip",           machine.getIp()                                        );
      currentRow.put("Port",              machine.getPort()                                      );
      currentRow.put("Number of ports",   machine.getNumberOfPorts()                             );
		}
		return true;
	}

	@Override
	protected CloseableIterator<Machine> list()
	{
		return DbMachines.listMachines();
	}

	public static DefaultTableModel createTableModel()
	{
    return new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Name", "Current Address", "Id", "Sharing", "Number of files", "Total files size", "Last Ip", "Port", "Number of ports"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, NumberOfFiles.class, DiskUsage.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false, false, false, false, false
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
	protected Machine[] createArray(int length)
	{
		return new Machine[length];
	}
}
