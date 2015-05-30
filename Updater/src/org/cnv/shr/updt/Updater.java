package org.cnv.shr.updt;

import java.io.File;
import java.net.ServerSocket;
import java.util.Timer;

import org.cnv.shr.util.KeysService;

public class Updater
{
	private static final long A_LONG_TIME = 7 * 24 * 60 * 60;
	
	static int KEY_LENGTH = 1024;
	static int UPDATE_PORT = 7005;
	static String UPDATE_DIRECTORY = "updates/";
	
	static File keysFile  = new File(UPDATE_DIRECTORY + UpdateInfoImpl.KEYS_TXT);
	static File propsFile = new File(UPDATE_DIRECTORY + UpdateInfoImpl.INFO_PROPS);

	static ServerSocket updateSocket;
	
	static KeysService service; 
	static UpdateThread updateThread;
	static Timer timer;
	static Code code;
	
	public static void main(String[] args) throws Exception
	{
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
