
package org.cnv.shr.dmn.mn;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.UpdateManager;
import org.cnv.shr.dmn.trk.Trackers;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.updt.UpdateInfo;
import org.cnv.shr.updt.UpdateInfoImpl;
import org.cnv.shr.util.KeysService;

public class GenerateKey
{
	private static final Path root = Paths.get("..", "instances", "updater");
	
	public static void main(String[] args) throws Exception
	{
		String ip = InetAddress.getLocalHost().getHostAddress();
		int port = UpdateInfo.DEFAULT_UPDATE_PORT;

		// Generate a new key for the updater
		Path keysFile = root.resolve(UpdateInfoImpl.KEYS_TXT);
		KeysService service = new KeysService();
		service.readKeys(keysFile, -1);
		service.createAnotherKey(keysFile, 1024);
		Path settingsFile = root.resolve("delme.txt");
		Services.settings = new Settings(settingsFile);

		// Save the generated key for the client
		UpdateManager updateManager = new UpdateManager(null);
		Services.settings.codeUpdateKey.set(root.resolve("updateKey"));
		updateManager.updateInfo(ip, port, service.getPublicKey());
		updateManager.write();
		if (Files.exists(settingsFile))
		{
			Files.delete(settingsFile);
		}
		
		Trackers trackers = new Trackers();
		trackers.add("127.0.0.1", TrackerEntry.TRACKER_PORT_BEGIN, TrackerEntry.TRACKER_PORT_END);
		trackers.save(Paths.get("..", "instances", "tracker", "trackers"));
		
		System.out.println("Done.");
	}
}
