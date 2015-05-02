package org.cnv.shr.test;

import org.cnv.shr.util.ByteListBuffer;
import org.cnv.shr.util.Misc;

public class TestByteListBuffer
{
	public static void main(String[] args)
	{
		byte[] naunce = Misc.getBytes(27);
		ByteListBuffer buffer = new ByteListBuffer();
		buffer.append(naunce);
		
		System.out.println(Misc.format(naunce));
		System.out.println(Misc.format(buffer.getBytes()));
		
		System.out.println(Misc.format(new ByteListBuffer().append(15).getBytes()));
	}
}
