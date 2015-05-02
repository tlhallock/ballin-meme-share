package org.cnv.shr.stng;

import java.awt.Container;

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
	Container createInput() {
		return null;
	}
}