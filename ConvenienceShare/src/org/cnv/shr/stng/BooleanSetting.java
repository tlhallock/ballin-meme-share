package org.cnv.shr.stng;

import java.awt.Container;

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
	Container createInput() {
		return null;
	}
	
}