package org.cnv.shr.stng;

import java.awt.Container;

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
	Container createInput() {
		return null;
	}
}