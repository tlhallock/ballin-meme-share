
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



package org.cnv.shr.cnctn;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.msg.Message;
import org.cnv.shr.msg.PermissionException;
import org.cnv.shr.util.ConnectionStatistics;
import org.cnv.shr.util.LogWrapper;

public class ConnectionRunnable implements Runnable
{
	Communication connection;
	Authenticator authentication;
	ConnectionStatistics stats;
	
	public ConnectionRunnable(final Communication c, final Authenticator authentication)
	{
		this.connection = c;
		this.authentication = authentication;
	}
	
	@Override
	public void run()
	{
		try
		{
			Services.networkManager.add(this);
			connection.initParser();
			while (connection.needsMore())
			{
				Message request = Services.msgReader.readMsg(connection.getParser(), connection.getUrl());
				
				if (request == null)
				{
					continue;
				}
				if (!authentication.authenticate(request))
				{
					break;
				}

				connection.setLastReceived(request.getJsonKey());
				
				try
				{
					request.perform(connection);
				}
				catch (final PermissionException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Permission error performing message task:" + request.getClass().getName(), e);
				}
				catch (final Exception e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Error performing message task:" + request.getClass().getName(), e);
					LogWrapper.getLogger().info("Closing connection.");
					break;
				}
			}
			LogWrapper.getLogger().info("No more input needed.");
		}
		catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, "Error creating message:", e);
			Services.quiter.quit();
		}
		catch (final Exception ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Error with connection:", ex);
		}
		finally
		{
			try
			{
				cleanup(false);
			}
			finally
			{
				Services.networkManager.remove(this);
			}
		}
	}

	void cleanup(boolean now)
	{
//		try
//		{
//			int ndx = 0;
//			while (!connection.isClosed() && !now && false)
//			{
//				if (ndx++ < 10)
//				{
//					Thread.sleep(200);
//					continue;
//				} 
//			}
//		}
//		catch (final InterruptedException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Interrupted", e);
//		}
//		finally
//		{
			try
			{
				connection.close();
				System.out.println("Closed");
			}
			catch (final Exception e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Can't close at all", e1);
			}
//		}
	}
}
