
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
