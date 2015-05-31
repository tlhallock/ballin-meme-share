package org.cnv.shr.gui.tbl;

import java.util.Date;
import java.util.HashMap;

import javax.swing.JTable;

import org.cnv.shr.db.h2.DbMessages;
import org.cnv.shr.gui.Application;
import org.cnv.shr.mdl.UserMessage;

public class MessageTable extends DbJTable<UserMessage>
{
	private Application app;

	public MessageTable(Application app, JTable table)
	{
		super(table);
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
		});
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
	protected org.cnv.shr.gui.tbl.DbJTable.MyIt<UserMessage> list()
	{
		return DbMessages.listMessages();
	}
}
