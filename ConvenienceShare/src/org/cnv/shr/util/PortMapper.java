package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.SettingListener;

public class PortMapper {
	static final String PORT_MAPPING_DESCRIPTION = "PortMapper";
	static final String portMapperJar = "lib/portmapper-2.0.0-alpha1.jar";
	static final Pattern EXISTING_MAPPING_PATTERN = Pattern
			.compile("TCP :([0-9]*) -> ([0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}):([0-9]*) enabled");

	static HashMap<Integer, Integer> listPorts() throws IOException,
			InterruptedException {
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

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));) {
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				if (!line.contains(PORT_MAPPING_DESCRIPTION)) {
					continue;
				}
				Matcher matcher = EXISTING_MAPPING_PATTERN.matcher(line);
				if (matcher.find()) {
					int key = Integer.parseInt(matcher.group(1));
					int value = Integer.parseInt(matcher.group(3));
					returnValue.put(key, value);
				}
			}
		} catch (IOException e) {
			LogWrapper.getLogger().log(Level.INFO, "Unable to list ports", e);
		}

		p.waitFor();

		return returnValue;

	}

	static boolean mapPort(Integer gateway, Integer local, String knownIp)
			throws IOException, InterruptedException {
		System.out.println("Mapping port " + gateway + " to " + local + " on "
				+ knownIp);
		LinkedList<String> arguments = new LinkedList<>();
		arguments.add("java");
		arguments.add("-jar");
		arguments.add(new File(portMapperJar).getAbsolutePath());
		arguments.add("-add");
		arguments.add("-externalPort");
		arguments.add(String.valueOf(gateway));
		arguments.add("-internalPort");
		arguments.add(String.valueOf(local));
		if (knownIp != null) {
			arguments.add("-ip");
			arguments.add(knownIp);
		}

		arguments.add("-protocol");
		arguments.add("tcp");
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(new File("."));
		builder.command(arguments);

		Process p = builder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("Shutdown registry")) {
					p.destroy();
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			LogWrapper.getLogger().log(Level.INFO, "Unable to map ports", e);
		}

		return listPorts().containsKey(new Integer(gateway));
	}

	static boolean removeMapping(int externalPort) throws InterruptedException,
			IOException {
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

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("Shutdown registry")) {
					p.destroy();
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			LogWrapper.getLogger().log(Level.INFO, "Unable to remove port mapping", e);
		}
		return !listPorts().containsKey(new Integer(externalPort));
	}

	static void removeAllMappings() throws IOException, InterruptedException {
		System.out.println("Removing port mappings.");
		HashMap<Integer, Integer> listPorts = listPorts();
		for (Integer remote : listPorts.keySet()) {
			removeMapping(remote);
		}
	}

	public static boolean currentRulesMatch(HashMap<Integer, Integer> ports) throws IOException,
			InterruptedException {
		HashMap<Integer, Integer> listPorts = listPorts();
		if (listPorts.size() != ports.size()) {
			return false;
		}

		for (Entry<Integer, Integer> port : ports.entrySet()) {
			Integer mapped = listPorts.get(new Integer(port.getKey()));
			if (mapped == null) {
				return false;
			}
			if (mapped != port.getValue()) {
				return false;
			}
		}
		return true;
	}

	static boolean map(HashMap<Integer, Integer> ports, String knownIp) throws IOException,
			InterruptedException {
		if (currentRulesMatch(ports)) {
			return true;
		}
		System.out.println("Current mappings did not match.");
		removeAllMappings();
		for (Entry<Integer, Integer> port : ports.entrySet()) {
			if (!mapPort(port.getKey(), port.getValue(), knownIp)) {
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

			int beginPortI = Services.settings.servePortBeginI.get();
			int beginPortE = Services.settings.servePortBeginE.get();
			
			int nports = Services.settings.maxServes.get();
			HashMap<Integer, Integer> mappings = new HashMap<>();
			for (int i = 0; i < nports; i++)
			{
				mappings.put(beginPortE + i, beginPortI + i);
			}
			try
			{
				if (!map(mappings, "idk what ip to use"))
				{
					LogWrapper.getLogger().info("That sux.");
				}
			}
			catch (IOException | InterruptedException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to change port mapping", e);
			}
		}
	}
}
