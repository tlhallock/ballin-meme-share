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