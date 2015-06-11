
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


package org.cnv.shr.sync;

import org.cnv.shr.mdl.PathElement;

public class Pair
{
	private FileSource fsCopy;
	private PathElement dbCopy;
	
	public Pair(final FileSource f, final PathElement pathElement)
	{
		fsCopy = f;
		dbCopy = pathElement;
	}
	
	FileSource getFsCopy()
	{
		return fsCopy;
	}
	
	public PathElement getPathElement()
	{
		return dbCopy;
	}

	public FileSource getSource()
	{
		return fsCopy;
	}
}
