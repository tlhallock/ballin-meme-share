
package org.cnv.shr.dmn.mn;

import java.io.File;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.UpdateManager;
import org.cnv.shr.stng.Settings;
import org.cnv.shr.updt.UpdateInfoImpl;
import org.cnv.shr.util.KeysService;

public class GenerateKey
{
	public static void main(String[] args) throws Exception
	{
		// Junk...
		String ip = "127.0.0.1";
		int port = 7005;

		// Generate a new key for the updater
		KeysService service = new KeysService();
		service.createAnotherKey(new File(UpdateInfoImpl.KEYS_TXT), 1024);
		File settingsFile = new File("delme.txt");
		Services.settings = new Settings(settingsFile);

		// Save the generated key for the client
		UpdateManager updateManager = new UpdateManager(null);
		try
		{
			updateManager.updateInfo(ip, port, service.getPublicKey());
		}
		catch (NullPointerException ex) {}
		updateManager.writeTo(new File("updateKey"));
		settingsFile.delete();
	}
}
