
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

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.gui.Application;
import org.cnv.shr.mdl.UserMessage;
import org.cnv.shr.util.CloseableIterator;

public class MessageTable extends DbJTable<UserMessage>
{
	private Application app;

	public MessageTable(Application app, JTable table)
	{
		super(table, "Id");
		this.app = app;
		
		addListener(new TableRightClickListener()
		{
			@Override
			void perform(UserMessage t)
			{
				t.open();
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
			void perform(UserMessage t)
			{
				DbMessages.deleteMessage(t.getId());
			}
			
			@Override
			String getName()
			{
				return "Delete";
			}
		});
	}

	@Override
	protected UserMessage create(HashMap<String, Object> currentRow)
	{
		return DbMessages.getMessage(Integer.parseInt((String) currentRow.get("Id")));
	}

	@Override
	protected void fillRow(UserMessage message, HashMap<String, Object> currentRow)
	{
		currentRow.put("Machine", message.getMachine().getName()           );
		currentRow.put("Type",    message.getMessageType().humanReadable() );
		currentRow.put("Date",    new Date(message.getSent())              );
		currentRow.put("Message", message.getMessage()                     );
		currentRow.put("Id",      String.valueOf(message.getId())          );
	}

	@Override
	protected CloseableIterator<UserMessage> list()
	{
		return DbMessages.listMessages();
	}

	public static DefaultTableModel createTableModel()
	{
		return new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Machine", "Type", "Date", "Message", "Id"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.util.Date.class, java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false, false, false, false
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
