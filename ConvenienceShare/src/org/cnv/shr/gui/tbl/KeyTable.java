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
