
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



package org.cnv.shr.stng;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class BooleanSetting extends Setting<Boolean>
{
	protected BooleanSetting(String n, Boolean dv, boolean r, boolean u,
			String d) {
		super(n, dv, r, u, d);
	}

	@Override
	Boolean parse(String vString) {
		return Boolean.valueOf(vString);
	}

	@Override
	public Component createInput()
	{
		final JCheckBox spinner = new JCheckBox();
		spinner.setSelected(get());
		spinner.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				boolean value = spinner.isSelected();
				if (value == get().booleanValue())
				{
					return;
				}
				set(value);
			}
		});
		addListener(new SettingListener()
		{
			@Override
			public void settingChanged()
			{
				boolean value = spinner.isSelected();
				if (value == get().booleanValue())
				{
					return;
				}
				spinner.setSelected(get().booleanValue());
			}
		});
		return spinner;
	}
	
}
