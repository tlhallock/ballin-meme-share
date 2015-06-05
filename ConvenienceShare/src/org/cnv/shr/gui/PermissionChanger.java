package org.cnv.shr.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.cnv.shr.db.h2.SharingState;


public abstract class PermissionChanger extends DefaultComboBoxModel<String> implements ActionListener
{
	public PermissionChanger(JComboBox box, SharingState current)
	{
		super(getOptions());
		setSelectedItem(current.humanReadable());
		box.addActionListener(this);
	}
	
	private SharingState getCurrentState()
	{
		String selectedItem = (String) this.getSelectedItem();
		
		for (SharingState state : SharingState.values())
		{
			if (selectedItem.equals(state.humanReadable()))
			{
				return state;
			}
		}
		return SharingState.DO_NOT_SHARE;
	}

	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		setPermission(getCurrentState());
	}
	
	private static String[] getOptions()
	{
		int permisssionLength = SharingState.values().length;
		String[] options = new String[permisssionLength];
		for (int i = 0; i < permisssionLength; i++)
		{
			options[i] = SharingState.values()[i].humanReadable();
		}
		return options;
	}
	
	protected abstract void setPermission(SharingState state);
}
