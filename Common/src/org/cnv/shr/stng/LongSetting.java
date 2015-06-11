
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

public class LongSetting extends Setting<Long>
{
	protected LongSetting(String n, Integer dv, boolean r, boolean u, String d) {
		super(n, (long) dv, r, u, d);
	}
	protected LongSetting(String n, Long dv, boolean r, boolean u, String d) {
		super(n, dv, r, u, d);
	}

	@Override
	Long parse(String vString) {
		return Long.parseLong(vString);
	}

	@Override
	public Component createInput()
	{
		JSpinner spinner = new JSpinner();
		final SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(get().intValue(), -1, Long.MAX_VALUE, 1);
		spinner.setModel(spinnerNumberModel);
		spinner.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				long value = spinnerNumberModel.getNumber().longValue();
				if (value == get().longValue())
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
				long value = spinnerNumberModel.getNumber().longValue();
				if (value == get().longValue())
				{
					return;
				}
				spinnerNumberModel.setValue(get().longValue());
			}
		});
		return spinner;
	}
}
