
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

package org.cnv.shr.gui.tbl;

import java.util.HashMap;

import javax.swing.JTable;

import org.cnv.shr.util.CloseableIterator;
import org.cnv.shr.util.KeyPairObject;

public class KeyTable extends DbJTable<KeyPairObject>
{

	public KeyTable(JTable table, String keyName)
	{
		super(table, keyName);
		
		
		// add listeners...
	}

	@Override
	protected KeyPairObject create(HashMap<String, Object> currentRow)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void fillRow(KeyPairObject t, HashMap<String, Object> currentRow)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected CloseableIterator<KeyPairObject> list()
	{
		// TODO Auto-generated method stub
		return null;
	}


}
