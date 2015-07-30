package org.cnv.shr.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.json.stream.JsonParser;

import org.chris.portmapper.PortMapperApp;
import org.chris.portmapper.model.PortMapping;
import org.chris.portmapper.model.Protocol;
import org.chris.portmapper.router.AbstractRouterFactory;
import org.chris.portmapper.router.IRouter;
import org.chris.portmapper.router.RouterException;
import org.chris.portmapper.router.cling.ClingRouterFactory;
import org.cnv.shr.prts.JsonPortMapping;
import org.cnv.shr.prts.PortMapArguments;
import org.cnv.shr.prts.PortMapperAction;
import org.cnv.shr.trck.TrackObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMapper3
{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private String routerFactoryClassName = ClingRouterFactory.class.getName();
	private Integer routerIndex = null;
	
	PortMapArguments arguments;
	
	PortMapper3(Path argumentsFile) throws IOException
	{
		try (InputStream input = Files.newInputStream(argumentsFile);
				 JsonParser parser = TrackObjectUtils.createParser(input, false);)
		{
			arguments = new PortMapArguments(parser);
		}
	}

	public void start()
	{
		if (arguments.upnpLib != null)
		{
			this.routerFactoryClassName = arguments.upnpLib;
			logger.info("Using router factory class '" + this.routerFactoryClassName + "'");
		}

		if (arguments.routerIndex != null)
		{
			this.routerIndex = arguments.routerIndex;
			logger.info("Using router index " + this.routerIndex);
		}

		try
		{
			IRouter router = connect();
			if (router == null)
			{
				logger.error("No router found: exit");
				System.exit(1);
				return;
			}
			PortMapperAction a = PortMapperAction.valueOf(arguments.action);
			switch (a)
			{
			case Add:
				for (JsonPortMapping mapping : arguments.ports)
				{
					addPortForwarding(router, mapping);
				}
				break;
			case Delete:
				for (JsonPortMapping mapping : arguments.ports)
				{
					deletePortForwardings(router, mapping);
				}
				break;
			case List:
				printPortForwardings(router);
				break;
			case PrintStatus:
				printStatus(router);
				break;
				default:
					System.err.println("Bad action:" + arguments.action);
			}
			router.disconnect();
		}
		catch (final RouterException e)
		{
			logger.error("An error occured", e);
			System.exit(1);
			return;
		}
	}

	private void printPortForwardings(final IRouter router) throws RouterException
	{
		final Collection<PortMapping> mappings = router.getPortMappings();
		if (mappings.size() == 0)
		{
			logger.info("No port mappings found");
			return;
		}
		final StringBuilder b = new StringBuilder();
		for (final Iterator<PortMapping> iterator = mappings.iterator(); iterator.hasNext();)
		{
			final PortMapping mapping = iterator.next();
			b.append(mapping.getCompleteDescription());
			if (iterator.hasNext())
			{
				b.append("\n");
			}
		}
		logger.info("Found " + mappings.size() + " port forwardings:\n" + b.toString());
	}
	
	private static Protocol getProtocol(String arg)
	{
		switch (arg)
		{
		case "TCP":
			return Protocol.TCP;
		case "UDP":
			return Protocol.UDP;
		default:
			throw new RuntimeException("Unknown protocol: " + arg);
		}
	}

	private void deletePortForwardings(final IRouter router, JsonPortMapping mapping) throws RouterException
	{

		final String remoteHost = null;
		final int port = mapping.externalPort;
		final Protocol protocol = getProtocol(mapping.protocol);

		logger.info("Deleting mapping for protocol " + protocol + " and external port " + port);
		router.removePortMapping(protocol, remoteHost, port);
		printPortForwardings(router);
	}

	private void printStatus(final IRouter router) throws RouterException
	{
		router.logRouterInfo();
	}
	
	private String getInternalIp(final IRouter router, JsonPortMapping jsonMapping)
	{
		try
		{
			return router.getLocalHostAddress();
		}
		catch (RouterException ex)
		{
			logger.info("Unable to get ip from router. Using " + jsonMapping.ip);
			logger.info("The exception was: ", ex);
		}
		return jsonMapping.ip;
	}

	private void addPortForwarding(final IRouter router, JsonPortMapping jsonMapping) throws RouterException
	{
		final String remoteHost = null;
		final String internalClient = getInternalIp(router, jsonMapping);
		final int internalPort = jsonMapping.internalPort;
		final int externalPort = jsonMapping.externalPort;
		final Protocol protocol = getProtocol(jsonMapping.protocol);

		String description = jsonMapping.description;
		if (description == null)
		{
			description = "PortMapper " + protocol + "/" + internalClient + ":" + internalPort;
		}
		final PortMapping mapping = new PortMapping(protocol, remoteHost, externalPort, internalClient, internalPort, description);
		logger.info("Adding mapping " + mapping);
		router.addPortMapping(mapping);
		printPortForwardings(router);
	}

	@SuppressWarnings("unchecked")
	private AbstractRouterFactory createRouterFactory() throws RouterException
	{
		Class<AbstractRouterFactory> routerFactoryClass;
		logger.info("Creating router factory for class {}", routerFactoryClassName);
		try
		{
			routerFactoryClass = (Class<AbstractRouterFactory>) Class.forName(routerFactoryClassName);
		}
		catch (final ClassNotFoundException e)
		{
			throw new RouterException("Did not find router factory class for name " + routerFactoryClassName, e);
		}

		logger.debug("Creating a new instance of the router factory class {}", routerFactoryClass);
		try
		{
			final Constructor<AbstractRouterFactory> constructor = routerFactoryClass.getConstructor(PortMapperApp.class);
			return constructor.newInstance(new PortMapperApp());
		}
		catch (final Exception e)
		{
			throw new RouterException("Error creating a router factory using class " + routerFactoryClass.getName(), e);
		}
	}

	private IRouter connect() throws RouterException
	{
		AbstractRouterFactory routerFactory;
		try
		{
			routerFactory = createRouterFactory();
		}
		catch (final RouterException e)
		{
			logger.error("Could not create router factory", e);
			return null;
		}
		logger.info("Searching for routers...");

		final List<IRouter> foundRouters = routerFactory.findRouters();

		return selectRouter(foundRouters);
	}

	/**
	 * @param foundRouters
	 * @return
	 */
	private IRouter selectRouter(final List<IRouter> foundRouters)
	{
		// One router found: use it.
		if (foundRouters.size() == 1)
		{
			final IRouter router = foundRouters.iterator().next();
			logger.info("Connected to router " + router.getName());
			return router;
		}
		else if (foundRouters.size() == 0)
		{
			logger.error("Found no router");
			return null;
		}
		else if (foundRouters.size() > 1 && routerIndex == null)
		{
			// let user choose which router to use.
			logger.error("Found more than one router. Use option -i <index>");

			int index = 0;
			for (final IRouter iRouter : foundRouters)
			{
				logger.error("- index " + index + ": " + iRouter.getName());
				index++;
			}
			return null;
		}
		else if (routerIndex >= 0 && routerIndex < foundRouters.size())
		{
			final IRouter router = foundRouters.get(routerIndex);
			logger.info("Found more than one router, using " + router.getName());
			return router;
		}
		else
		{
			logger.error("Index must be between 0 and " + (foundRouters.size() - 1));
			return null;
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		try
		{
			Path argumentsFile;
			if (args.length < 1)
			{
				argumentsFile = Paths.get("");
			}
			else
			{
				argumentsFile = Paths.get(args[0]);
			}
			new PortMapper3(argumentsFile).start();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		finally
		{
			System.exit(0);
		}
	}
}
