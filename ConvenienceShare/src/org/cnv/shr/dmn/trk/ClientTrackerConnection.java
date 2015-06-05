package org.cnv.shr.dmn.trk;

import java.io.IOException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.util.Misc;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class ClientTrackerConnection extends TrackerConnection
{
	RSAPublicKey publicKey;
	
	ClientTrackerConnection(String url, int port) throws IOException
	{
		super(url, port);
	}

	@Override
	protected MachineEntry getLocalMachine()
	{
		publicKey = Services.keyManager.getPublicKey();
		return new MachineEntry(
				Services.settings.machineIdentifier.get(),
				publicKey,
				"not used.",
				Services.settings.servePortBeginE.get(),
				Services.settings.servePortBeginE.get() + Services.settings.numThreads.get(),
				Services.settings.machineName.get());
	}

	@Override
	protected void sendDecryptedNaunce(byte[] naunceRequest, RSAPublicKey publicKey2)
	{
		if (publicKey2 == null)
		{
			publicKey2 = publicKey;
		}
		generator.write("decrypted", Misc.format(Services.keyManager.decrypt(publicKey, naunceRequest)));
	}
}
