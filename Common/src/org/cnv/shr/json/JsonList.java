
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

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.Jsonable;

public class JsonList<T extends Jsonable> extends LinkedList<T> 
{
	private Allocator<T> allocator;
	
	public interface Allocator<T>
	{
		T create(JsonParser parser);
	}
	
	public JsonList(Allocator<T> allocator)
	{
		this.allocator = allocator;
	}

	public void generate(JsonGenerator generator)
	{
//		generator.writeStartArray();
		
		for (T t : this)
		{
			t.generate(generator, null);
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
			case START_OBJECT:
				add(allocator.create(parser));
				break;
			case END_ARRAY:
				return;
			}
		}
	}
	
	public String toDebugString() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(output, true);) {
			generate(generator);
		}
		return new String(output.toByteArray());
	}
}
