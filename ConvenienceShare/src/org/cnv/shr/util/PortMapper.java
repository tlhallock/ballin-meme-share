package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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

	public static boolean currentRulesMatch(int[] ports) throws IOException,
			InterruptedException {
		HashMap<Integer, Integer> listPorts = listPorts();
		if (listPorts.size() != ports.length) {
			return false;
		}

		for (int port : ports) {
			Integer mapped = listPorts.get(new Integer(port));
			if (mapped == null) {
				return false;
			}
			if (mapped != port) {
				return false;
			}
		}
		return true;
	}

	static boolean map(int[] ports, String knownIp) throws IOException,
			InterruptedException {
		if (currentRulesMatch(ports)) {
			return true;
		}
		System.out.println("Current mappings did not match.");
		removeAllMappings();
		for (int port : ports) {
			if (!mapPort(port, port, knownIp)) {
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

			int beginPort = Services.settings.servePortBegin.get();
			int nports = Services.settings.servePortBegin.get();

			int[] ports = new int[nports];
			for (int i = 0; i < nports; i++)
			{
				ports[i] = beginPort + i;
			}
			try
			{
				if (!map(ports, "idk what ip to use"))
				{
					Services.logger.println("That sux.");
				}
			}
			catch (IOException | InterruptedException e)
			{
				Services.logger.print(e);
			}
		}
	}
}
