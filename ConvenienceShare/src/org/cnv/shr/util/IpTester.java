package org.cnv.shr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpTester
{
	private static Pattern pattern = Pattern.compile(
                "<div[^>]*>[ \t\n]*" + 
                	"<label for=\"ip\">Your IP:</label>[ \t\n]*" +
                    "<input id=\"ip\" type=\"text\" value=\"([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\"[^>]*>[ \t\n]*" + 
                "</div>");
	
	public String getIpFromCanYouSeeMeDotOrg()
	{
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL("http://canyouseeme.org/").openStream()));)
		{
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				builder.append(line);
			}
			Matcher matcher = pattern.matcher(builder);
			if (matcher.find())
			{
				return matcher.group(1);
			}
			return null;
		}
		catch (MalformedURLException e)
		{
			LogWrapper.getLogger().log(Level.FINE, "Bad canyouseeme url", e);
			return null;
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.FINE, "Unable to connect to canyouseeme", e);
			return null;
		}
	}
	
	public String testIp(String ip, int port)
	{
		try (Socket s = new Socket(ip, port);)
		{
			// I guess this is not that useful.
			return "Able to connect locally.";
		}
		catch (NoRouteToHostException e)
		{
			LogWrapper.getLogger().log(Level.FINE, "No route to host.", e);
			return "No route to host.";
		}
		catch (UnknownHostException e)
		{
			LogWrapper.getLogger().log(Level.FINE, "Unkown host.", e);
			return "Unkown host.";
		}
		catch (java.net.ConnectException e)
		{
			LogWrapper.getLogger().log(Level.FINE, "Port not open. (Is it forwarded?)", e);
			return "Port not open. (Is it forwarded?)";
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.FINE, "Unable to read.", e);
			return "Unable to read.";
		}
	}
}
