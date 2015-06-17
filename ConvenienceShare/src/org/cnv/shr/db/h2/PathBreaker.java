
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



package org.cnv.shr.db.h2;

import java.util.LinkedList;

import org.cnv.shr.mdl.PathElement;
import org.cnv.shr.util.Misc;

public class PathBreaker
{
	public static int PATH_ELEMENT_LENGTH = 20;
	
	private static final PathElement[] dummy = new PathElement[0];

	public static PathElement[] breakPath(PathElement parent, String path)
	{
		path = Misc.sanitizePath(path);
		
		LinkedList<PathElement> returnValue = new LinkedList<>();
		
		StringBuilder builder = new StringBuilder(PATH_ELEMENT_LENGTH);
		
		int idx = 0;
		
		outer:
		while (idx < path.length())
		{
			for (int i = 0; i < PATH_ELEMENT_LENGTH && idx < path.length(); i++)
			{
				char c = path.charAt(idx++);
				builder.append(c);
				if (c == '/')
				{
					returnValue.add(parent = new PathElement(parent, builder.toString(), false));
					builder.setLength(0);
					continue outer;
				}
			}
			if (idx == path.length())
			{
				returnValue.add(parent = new PathElement(parent, builder.toString(), false));
				builder.setLength(0);
				return returnValue.toArray(dummy);
			}
			returnValue.add(parent = new PathElement(parent, builder.toString(), true));
			builder.setLength(0);
		}
		
		return returnValue.toArray(dummy);
	}
	
	public static PathElement[] breakPath(String path)
	{
		return breakPath(DbPaths.ROOT, path);
	}

	public static String join(PathElement[] eles)
	{
		StringBuilder builder = new StringBuilder(eles.length * PATH_ELEMENT_LENGTH);
		
		for (PathElement ele : eles)
		{
//			builder.append("[");
			builder.append(ele.getName());
//			if (ele.isBroken())
//			{
//				builder.append("...");
//			}
//			builder.append("]");
		}
		
		return builder.toString();
	}
}
