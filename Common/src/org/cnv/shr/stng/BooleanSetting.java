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