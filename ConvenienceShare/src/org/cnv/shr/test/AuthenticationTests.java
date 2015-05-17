package org.cnv.shr.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;

import org.cnv.shr.db.h2.DbKeys;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.junit.Assert;
import org.junit.Test;

public class AuthenticationTests extends RemotesTest
{
	// list keys

	@Test
	public void testCanAddOther() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);
			 Closeable c1 = getMachineInfo(0).launch(true);)
		{
			UserActions.addMachine(getMachineInfo(0).getUrl());
			
			Thread.sleep(5000);
			
			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}
	}

	@Test
	public void testOtherAdd() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true); 
			 Closeable c1 = getMachineInfo(0).launch(true);)
		{
			getMachineInfo(0).send(new TestActions.AddMachine(getLocalUrl()));
			Thread.sleep(5000);

			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}
	}

	@Test
	public void testCanAddOtherWithExistingKey() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);
			 Closeable c1 = getMachineInfo(0).launch(true);)
		{
			UserActions.addMachine(getMachineInfo(0).getUrl());
			
			Thread.sleep(5000);
			
			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}
		
		try (Closeable c2 = launchLocalMachine(false);
			 Closeable c1 = getMachineInfo(0).launch(false);)
		{
			String ident = getMachineInfo(0).getIdent();
			Machine machine = DbMachines.getMachine(ident);
			Assert.assertNotNull(machine);
			UserActions.syncRoots(machine);
			
			Thread.sleep(5000);
			
			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}
	}
	
	@Test
	public void testOtherAddWithExistingKey() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true); 
			 Closeable c1 = getMachineInfo(0).launch(true);)
		{
			getMachineInfo(0).send(new TestActions.AddMachine(getLocalUrl()));
			Thread.sleep(5000);

			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}

		try (Closeable c2 = launchLocalMachine(false); 
			 Closeable c1 = getMachineInfo(0).launch(false);)
		{
			getMachineInfo(0).send(new TestActions.SYNC_ROOTS(Services.localMachine.getIdentifier()));
			Thread.sleep(5000);

			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}
	}

	@Test
	public void machineHasAddedKey() throws Exception
	{
		try (Closeable c2 = launchLocalMachine(true);)
		{
			Assert.assertTrue(DbKeys.machineHasKey(Services.localMachine, Services.keyManager.getPublicKey()));
		}
	}
}
