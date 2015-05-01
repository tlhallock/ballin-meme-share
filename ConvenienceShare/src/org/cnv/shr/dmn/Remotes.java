package org.cnv.shr.dmn;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.msg.FindMachines;
import org.cnv.shr.msg.MachineFound;
import org.json.JSONArray;

public class Remotes
{
	public void refresh()
	{
		for (Machine machine : getMachines())
		{
			machine.refresh();
		}
	}

	public void discover(final String url)
	{
		try
		{
			Connection openConnection = Services.networkManager.openConnection(url);
			openConnection.send(new MachineFound());
			openConnection.send(new FindMachines());
			openConnection.notifyDone();
		}
		catch (IOException e)
		{
			Services.logger.logStream.println("Unable to discover " + url);
			e.printStackTrace(Services.logger.logStream);
		}
	}

	public List<Machine> getMachines()
	{
		return Services.db.getRemoteMachines();
	}
	
	public String[] getKeys(String ip, int port)
	{
		return null;
	}
	
	public void addMachine(Machine m)
	{
		Services.logger.logStream.println("Adding remote " + m.getIp() + ":" + m.getPort());
		try
		{
			Services.db.addMachine(m);
		}
		catch (SQLException e)
		{
			Services.logger.logStream.println("Unable to add machine.");
			e.printStackTrace(Services.logger.logStream);
		}
		Services.notifications.remotesChanged();
	}

	public void isAlive(Machine machine)
	{
		
	}
	
	
	public void debug(PrintStream ps)
	{
		try
		{
			JSONArray arr = new JSONArray();
			for (Machine machine : getMachines())
			{
				machine.append(arr);
			}
			ps.println(arr.toString(8));
		}
		catch (Exception e)
		{
			Services.logger.logStream.println("Unable to save Remotes.");
			Services.logger.logStream.println("This is expected on first run.");
			e.printStackTrace(Services.logger.logStream);
		}
	}
	
	/**
	public List<Machine> read()
	{
		LinkedList<Machine> machines = new LinkedList<>();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(Services.settings.getRemotesFile())))
		{
			JSONArray arr = new JSONArray(new JSONTokener(reader));
			for (int i = 0; i < arr.length(); i++)
			{
				machines.add(new Machine(arr.getJSONObject(i)));
			}
		}
		catch (Exception e)
		{
			Services.logger.logStream.println("Unable to read Remotes.");
			Services.logger.logStream.println("This is expected on first run.");
			e.printStackTrace(Services.logger.logStream);
		}

		return machines;
	}
	**/
}
