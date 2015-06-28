/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */

package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.SettingListener;

public class PortMapper
{
	public static final String portMapperJar = "lib/portmapper-2.0.0-alpha1.jar";
	static final String PORT_MAPPING_DESCRIPTION = "PortMapper";
	static final Pattern EXISTING_MAPPING_PATTERN = Pattern.compile(
			"TCP :([0-9]*) -> ([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}):([0-9]*) enabled");

	// I am guessing this is supposed to be the 192.168.0.x, which is why this is
	// not workings...

	static HashMap<Integer, Integer> listPorts(PrintStream logStream) throws IOException, InterruptedException
	{
		HashMap<Integer, Integer> returnValue = new HashMap<>();

		LinkedList<String> arguments = new LinkedList<>();
		arguments.add("java");
		arguments.add("-jar");
		arguments.add(new File(portMapperJar).getAbsolutePath());
		arguments.add("-list");
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File("."));
		builder.command(arguments);
		final Process p = builder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));)
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				logStream.println(line);
				if (!line.contains(PORT_MAPPING_DESCRIPTION))
				{
					continue;
				}
				Matcher matcher = EXISTING_MAPPING_PATTERN.matcher(line);
				if (matcher.find())
				{
					int key = Integer.parseInt(matcher.group(1));
					int value = Integer.parseInt(matcher.group(3));
					returnValue.put(key, value);
				}
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to list ports", e);
		}

		p.waitFor();

		return returnValue;

	}

	static boolean mapPort(Integer gateway, Integer local, String knownIp, PrintStream logStream) throws IOException, InterruptedException
	{
		logStream.println("Mapping port " + gateway + " to " + local + " on " + knownIp);
		LinkedList<String> arguments = new LinkedList<>();
		arguments.add("java");
		arguments.add("-jar");
		arguments.add(new File(portMapperJar).getAbsolutePath());
		arguments.add("-add");
		arguments.add("-externalPort");
		arguments.add(String.valueOf(gateway));
		arguments.add("-internalPort");
		arguments.add(String.valueOf(local));
		if (knownIp != null)
		{
			arguments.add("-ip");
			arguments.add(knownIp);
		}

		arguments.add("-protocol");
		arguments.add("tcp");
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File("."));
		builder.command(arguments);

		Process p = builder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));)
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.contains("Shutdown registry"))
				{
					p.destroy();
				}
				logStream.println(line);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to map ports", e);
		}

		return listPorts(logStream).containsKey(new Integer(gateway));
	}

	static boolean removeMapping(int externalPort, PrintStream logStream) throws InterruptedException, IOException
	{
		LinkedList<String> arguments = new LinkedList<>();
		arguments.add("java");
		arguments.add("-jar");
		arguments.add(new File(portMapperJar).getAbsolutePath());
		arguments.add("-delete");
		arguments.add("-externalPort");
		arguments.add(String.valueOf(externalPort));
		arguments.add("-protocol");
		arguments.add("tcp");
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File("."));
		builder.command(arguments);

		Process p = builder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));)
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.contains("Shutdown registry"))
				{
					p.destroy();
				}
				logStream.println(line);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove port mapping", e);
		}
		return !listPorts(logStream).containsKey(new Integer(externalPort));
	}

	public static void removeAllMappings(PrintStream logStream) throws IOException, InterruptedException
	{
		logStream.println("Removing port mappings.");
		HashMap<Integer, Integer> listPorts = listPorts(logStream);
		for (Integer remote : listPorts.keySet())
		{
			removeMapping(remote, logStream);
		}
	}

	public static boolean currentRulesMatch(HashMap<Integer, Integer> ports, PrintStream logStream) throws IOException, InterruptedException
	{
		HashMap<Integer, Integer> listPorts = listPorts(logStream);
		if (listPorts.size() != ports.size())
		{
			return false;
		}

		for (Entry<Integer, Integer> port : ports.entrySet())
		{
			Integer mapped = listPorts.get(new Integer(port.getKey()));
			if (mapped == null)
			{
				return false;
			}
			if (mapped != port.getValue())
			{
				return false;
			}
		}
		return true;
	}

	public static void addDesiredPorts(String ipAddress, PrintStream logStream)
	{
		int beginPortI = Services.settings.servePortBeginI.get();
		int beginPortE = Services.settings.servePortBeginE.get();

		int nports = Services.settings.numHandlers.get();
		HashMap<Integer, Integer> mappings = new HashMap<>();
		for (int i = 0; i < nports; i++)
		{
			mappings.put(beginPortE + i, beginPortI + i);
		}
		try
		{
			if (!map(mappings, ipAddress, logStream))
			{
				LogWrapper.getLogger().info("That sux.");
			}
		}
		catch (IOException | InterruptedException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to change port mapping", e);
		}
	}

	static boolean map(HashMap<Integer, Integer> ports, String knownIp, PrintStream logStream) throws IOException, InterruptedException
	{
		if (currentRulesMatch(ports, logStream))
		{
			return true;
		}
		logStream.println("Current mappings did not match.");
		removeAllMappings(logStream);
		for (Entry<Integer, Integer> port : ports.entrySet())
		{
			if (!mapPort(port.getKey(), port.getValue(), knownIp, logStream))
			{
				return false;
			}
		}
		return true;
	}

	public static class PortListener implements SettingListener
	{
		@Override
		public void settingChanged()
		{
			// restart server threads too...
			addDesiredPorts(Services.settings.getLocalIp(), System.out);
		}
	}
}
