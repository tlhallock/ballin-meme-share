
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

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class IntSetting extends Setting<Integer>
{
	private int min;
	private int max;
	
	protected IntSetting(String n, Integer dv, boolean r, boolean u, String d)
	{
		this(n, dv, Integer.MIN_VALUE, Integer.MAX_VALUE, r, u, d);
	}
	
	protected IntSetting(String n, Integer dv, int min, int max, boolean r, boolean u, String d) {
		super(n, dv, r, u, d);
		this.min = min;
		this.max = max;
		set(dv);
	}
	
	protected void sanitize()
	{
		value = Math.min(max, Math.max(min,  value));
	}
	
	@Override
	Integer parse(String vString) {
		return Integer.parseInt(vString);
	}

	@Override
	public Component createInput()
	{
		JSpinner spinner = new JSpinner();
		final SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(get().intValue(), min, max, 1);
		spinner.setModel(spinnerNumberModel);
		spinner.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				int value = spinnerNumberModel.getNumber().intValue();
				if (value == get().intValue())
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
				int value = spinnerNumberModel.getNumber().intValue();
				if (value == get().intValue())
				{
					return;
				}
				spinnerNumberModel.setValue(get().intValue());
			}
		});
		return spinner;
	}
}
