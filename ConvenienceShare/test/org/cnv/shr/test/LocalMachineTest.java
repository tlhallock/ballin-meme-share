package org.cnv.shr.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
		Path root = Paths.get("..", "instances", "localInstance");
		Settings stgs = new Settings( root.resolve("settings.props"));
		stgs.applicationDirectory.set(root.resolve("app"));
		stgs.downloadsDirectory.set(  root.resolve("downloads"));
		stgs.setDefaultApplicationDirectoryStructure();
		
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
			args.showGui = true;
		
			Services.isAlreadyRunning(args);
		Services.initialize(args, null);

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
