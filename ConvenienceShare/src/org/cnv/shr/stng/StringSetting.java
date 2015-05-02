package org.cnv.shr.stng;

import java.awt.Container;

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
	Container createInput() {
		return null;
	}
}