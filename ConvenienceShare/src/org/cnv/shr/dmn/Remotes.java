package org.cnv.shr.dmn;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RemoteDirectory;
import org.cnv.shr.mdl.User;
import org.json.JSONArray;
import org.json.JSONTokener;

public class Remotes
{
	LinkedList<Machine> machines = new LinkedList<>();
	HashMap<Machine, List<RemoteDirectory>> sharedDirectories = new HashMap<>();

	public void refresh(String ip)
	{

	}

	public User discover(String ip, int port)
	{
		return null;

	}

	public void isAlive(Machine machine)
	{

	}

	public List<Machine> getMachines()
	{
		return machines;
	}
	
	public void write()
	{
		try (PrintStream ps = new PrintStream(new FileOutputStream(Services.settings.getRemotesFile())))
		{
			JSONArray arr = new JSONArray();
			for (Machine machine : machines)
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
	
	public void read()
	{
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
		
		Notifications.remotesChanged();
	}
}
