package org.cnv.shr.test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.mn.Arguments;
import org.cnv.shr.dmn.mn.Quiter;
import org.cnv.shr.stng.Settings;


public class LocalMachineTest
{
	private static int LOCAL_PORT = 6990;
	protected boolean quit;
	
	public Closeable launchLocalMachine(boolean deleteDb) throws Exception
	{
		String root = ".." + File.separator + "instances" + File.separator + "localInstance";
		Settings stgs = new Settings( new File(root + File.separator + "settings.props"));
		stgs.applicationDirectory.set(new File(root + File.separator + "app"));
		stgs.logFile.set(             new File(root + File.separator + "app" + File.separator + "log.txt"));
		stgs.keysFile.set(            new File(root + File.separator + "app" + File.separator + "keys.txt"));
		stgs.dbFile.set(              new File(root + File.separator + "app" + File.separator + "files_database"));
		stgs.downloadsDirectory.set(  new File(root + File.separator + "downloads"));
		stgs.servingDirectory.set(    new File(root + File.separator + "serve"));
		stgs.stagingDirectory.set(    new File(root + File.separator + "stage"));
		stgs.servePortBeginI.set(LOCAL_PORT);
		stgs.servePortBeginE.set(LOCAL_PORT);
		stgs.machineIdentifier.set("6mkDuKhkiTpjpM3vS6LGEKN72dEB4tmsaKslTJc2ZDrXLGplYE");
		stgs.shareWithEveryone.set(true);
		stgs.write();
		
		Arguments args = new Arguments();
		args.settings = stgs;
		args.deleteDb = deleteDb;
		args.quiter = new Quiter() {
			@Override
			public void doFinal()
			{
				quit = true;
			}};
		
		Services.initialize(args);

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
