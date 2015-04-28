package org.cnv.shr.mdl;

import java.util.Collection;
import java.util.HashSet;

public class User
{
	HashSet<Machine> machines = new HashSet<>();
	String userId;

	public Collection<Machine> getMachines()
	{
		return machines;
	}
}
