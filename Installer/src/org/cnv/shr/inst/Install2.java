package org.cnv.shr.inst;

import java.io.IOException;

import org.cnv.shr.gui.NewMachineFrame;

public class Install2
{
	public static void main(String[] args) throws IOException, InterruptedException
	{
		NewMachineFrame newMachineFrame = new NewMachineFrame();
		newMachineFrame.setVisible(true);
		newMachineFrame.installSneaky("1", 7990);
		newMachineFrame.installSneaky("2", 8990);
		Thread.sleep(10000);
		System.exit(0);
	}
}
