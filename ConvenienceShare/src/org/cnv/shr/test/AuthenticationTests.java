package org.cnv.shr.test;

import java.io.Closeable;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.gui.UserActions;
import org.junit.Assert;
import org.junit.Test;

public class AuthenticationTests extends RemotesTest
{
	// list keys

	@Test
	public void testCanAddOther() throws Exception
	{
		try (Closeable c1 = getMachineInfo(0).launch();
			 Closeable c2 = launchLocalMachine();)
		{
			UserActions.addMachine(getMachineInfo(0).getUrl());
			
			Thread.sleep(5000);
			
			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(Main.quitting);
		}
	}

	@Test
	public void testOtherAdd() throws Exception
	{
		try (Closeable c1 = getMachineInfo(0).launch(); 
			 Closeable c2 = launchLocalMachine();)
		{
			getMachineInfo(0).send(new TestActions.AddMachine(getLocalUrl()));
			Thread.sleep(5000);

			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(Main.quitting);
		}
	}
}
