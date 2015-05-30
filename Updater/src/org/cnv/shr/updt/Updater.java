package org.cnv.shr.updt;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;

import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.Misc;

public class Updater
{
	private static final long A_LONG_TIME = 7 * 24 * 60 * 60;
	
	static int KEY_LENGTH = 1024;
	static int UPDATE_PORT = 7005;
	static String ROOT_DIRECTORY = "../instances/updater/";
	
	static Path keysFile;
	static Path propsFile;

	static ServerSocket updateSocket;
	
	static KeysService service; 
	static UpdateThread updateThread;
	static Timer timer;
	static Code code;
	
	static String getUpdatesDirectory()
	{
		return ROOT_DIRECTORY + "updates/";
	}
	
	public static void main(String[] args) throws Exception
	{
		if (args.length > 0)
			ROOT_DIRECTORY = args[0];
		Misc.ensureDirectory(new File(getUpdatesDirectory()), false);

		keysFile  = Paths.get(ROOT_DIRECTORY, UpdateInfoImpl.KEYS_TXT);
		propsFile = Paths.get(ROOT_DIRECTORY, UpdateInfoImpl.INFO_PROPS);
		
		updateSocket = new ServerSocket(Updater.UPDATE_PORT);
		code = new Code();
		service = new KeysService();
		service.readKeys(keysFile, KEY_LENGTH);
		updateThread = new UpdateThread();
		UpdateInfoImpl.write(propsFile, updateSocket.getInetAddress().getHostAddress(), updateSocket.getLocalPort());
		updateThread.start();
		timer.scheduleAtFixedRate(new KeyUpdater(), A_LONG_TIME, A_LONG_TIME);
	}
}
