package org.cnv.shr.dmn;

import org.cnv.shr.db.DbConnection;
import org.cnv.shr.mdl.Machine;

public class Main
{
	public static void main(String[] args) throws Exception
	{
		Services.initialize();

		Services.remotes.getMachines().add(new Machine("20.43.98.43:032984"));
		
		DbConnection.initialize();
	}

	public static void quit()
	{
		Services.deInitialize();
		System.exit(0);
	}
}
