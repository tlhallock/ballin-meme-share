package org.cnv.shr.test;

import java.io.File;
import java.net.UnknownHostException;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class RemotesTest extends LocalMachineTest
{
	static MachineInfo[] infos;
	
	@BeforeClass
	public static void createRemotes() throws UnknownHostException
	{
		infos = new MachineInfo[2];
		infos[0] = new MachineInfo("bin" + File.separator + "instance1", 7990, "clOvFbFlEEDh0sfCQieGwbwCv2E6T9sBarPnJ0UV52zb8XDn1f");
		infos[1] = new MachineInfo("bin" + File.separator + "instance2", 8990, "KroGnSns2whGXu5ihtxlgjdr8Xg4YxuJjS5oKsq2DS8NeLn046");
	}

	public MachineInfo getMachineInfo(int ndx)
	{
		return infos[ndx];
	}

	
	public void assertKnowMachine(String ident)
	{
		Assert.assertTrue("Do not have the machine.", DbMachines.getMachine(ident) != null);
	}
	
	public void assertHasAKeyFor(String ident)
	{
		Assert.assertTrue("Machine does not have a key", DbKeys.getKey(DbMachines.getMachine(ident)) != null);
	}
	
	@AfterClass
	public static void destroyRemotes()
	{
		for (MachineInfo info : infos)
		{
			info.kill();
		}
	}
}
