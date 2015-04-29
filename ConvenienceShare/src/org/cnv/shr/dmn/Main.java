package org.cnv.shr.dmn;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.cnv.shr.mdl.Machine;
import org.json.JSONException;

public class Main
{
	public static void main(String[] args) throws FileNotFoundException, 
		IOException, InterruptedException, JSONException
	{
		Services.initialize();

		Services.remotes.getMachines().add(new Machine("20.43.98.43:032984"));
	}

	public static void quit()
	{
		Services.deInitialize();
		System.exit(0);
	}
}
