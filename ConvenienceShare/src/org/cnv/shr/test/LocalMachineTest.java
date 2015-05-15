package org.cnv.shr.test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.cnv.shr.dmn.Main;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.stng.Settings;


public class LocalMachineTest
{
	private static int LOCAL_PORT = 6990;
	
	public Closeable launchLocalMachine() throws Exception
	{
		String root = "bin" + File.separator + "localInstance";
		Settings stgs = new Settings( new File(root + File.separator + "settings.props"));
		stgs.applicationDirectory.set(new File(root + File.separator + "app"));
		stgs.logFile.set(             new File(root + File.separator + "app" + File.separator + "log.txt"));
		stgs.keysFile.set(            new File(root + File.separator + "app" + File.separator + "keys.txt"));
		stgs.dbFile.set(              new File(root + File.separator + "app" + File.separator + "files_database"));
		stgs.downloadsDirectory.set(  new File(root + File.separator + "downloads"));
		stgs.servingDirectory.set(    new File(root + File.separator + "serve"));
		stgs.stagingDirectory.set(    new File(root + File.separator + "stage"));
		stgs.servePortBegin.set(LOCAL_PORT);
		stgs.machineIdentifier.set("6mkDuKhkiTpjpM3vS6LGEKN72dEB4tmsaKslTJc2ZDrXLGplYE");
		Main.quitting = false;
		Services.initialize(stgs, true);

		return new Closeable() {
			@Override
			public void close() throws IOException
			{
				shutdownLocalMachine();
			}};
	}
	
	public void shutdownLocalMachine()
	{
		Services.deInitialize();
	}
	
	public String getLocalUrl() throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostAddress() + ":" + LOCAL_PORT;
	}
}
