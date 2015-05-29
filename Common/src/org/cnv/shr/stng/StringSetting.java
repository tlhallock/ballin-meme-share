package org.cnv.shr.stng;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

public class StringSetting extends Setting<String> {

	protected StringSetting(String n, String dv, boolean r, boolean u,
			String d) {
		super(n, dv, r, u, d);
	}

	@Override
	String parse(String vString) {
		return vString;
	}

	@Override
	public Component createInput() {
		final JTextField field = new JTextField();
		field.setText(get().toString());
		addListener(new SettingListener()
		{
			@Override
			public void settingChanged()
			{
				field.setText(get());
			}
		});
		
		field.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				set(field.getText());
			}});
		
		return field;
	}
}