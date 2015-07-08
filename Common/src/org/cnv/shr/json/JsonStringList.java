
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



package org.cnv.shr.json;

import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

public class JsonStringList extends LinkedList<String> 
{
	public JsonStringList() {}

	public void generate(JsonGenerator generator)
	{
//		generator.writeStartArray();
		
		for (String t : this)
		{
			generator.write(t);
		}
		
		generator.writeEnd();
	}

	public void parse(JsonParser parser)
	{
		clear();
		while (parser.hasNext())
		{
			switch (parser.next())
			{
			case VALUE_STRING:
				add(parser.getString());
				break;
			case END_ARRAY:
				return;
			}
		}
	}
}
