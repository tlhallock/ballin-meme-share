package org.cnv.shr.dmn;

import org.cnv.shr.mdl.Machine;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		Services.initialize(new String[] {});

		Services.remotes.getMachines().add(new Machine("20.43.98.43:032984"));
		Machine m = new Machine("234.234.234.234:99");
		Services.db.addMachine(m);
		
		Thread.sleep(5000);
		
		Services.application.showRemote(m);
	}

	public static void quit()
	{
		Services.deInitialize();
		System.exit(0);
	}
}
