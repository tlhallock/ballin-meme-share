package org.cnv.shr.test;

import java.io.Closeable;

import org.cnv.shr.gui.UserActions;
import org.junit.Assert;
import org.junit.Test;

public class AuthenticationTests extends RemotesTest
{
	// list keys

	@Test
	public void testCanAddOther() throws Exception
	{
		try (Closeable c2 = launchLocalMachine();
			 Closeable c1 = getMachineInfo(0).launch();)
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
		try (Closeable c2 = launchLocalMachine(); 
			 Closeable c1 = getMachineInfo(0).launch();)
		{
			getMachineInfo(0).send(new TestActions.AddMachine(getLocalUrl()));
			Thread.sleep(5000);

			assertKnowMachine(getMachineInfo(0).getIdent());
			assertHasAKeyFor( getMachineInfo(0).getIdent());
			
			Assert.assertFalse(quit);
		}
	}
}
