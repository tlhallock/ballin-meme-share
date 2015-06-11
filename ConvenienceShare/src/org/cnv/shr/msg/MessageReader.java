
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


package org.cnv.shr.msg;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.json.stream.JsonParser;

import org.cnv.shr.json.JsonAllocators;
import org.cnv.shr.util.Jsonable;
import org.cnv.shr.util.LogWrapper;



public class MessageReader
{
	public Message readMsg(JsonParser input) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		String className = null;
		wh:
		while (input.hasNext())
		{
			JsonParser.Event e = input.next();
			switch (e)
			{
			case KEY_NAME:
				className = input.getString();
				break;
			case START_OBJECT:
				break wh;
			}
		}

		LogWrapper.getLogger().info("Received message of type " + className);
		Jsonable create = JsonAllocators.create(className, input);
		if (create == null)
		{
			LogWrapper.getLogger().info("Ignoring unknown message type: " + className + " from ");
			return null;
		}
		if (!(create instanceof Message))
		{
			LogWrapper.getLogger().info("Received something that is not a message: " + className);
			return null;
		}
		LogWrapper.getLogger().info("Read " + create);
		return (Message) create;
	}
}
