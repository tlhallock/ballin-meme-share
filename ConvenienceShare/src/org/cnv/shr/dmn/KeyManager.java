package org.cnv.shr.dmn;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

public class KeyManager
{
	LinkedList<KeyPair> keys = new LinkedList<>();
	
	void generateKeys()
	{
		KeyPairGenerator generator = null;
		try
		{
			generator = KeyPairGenerator.getInstance(Settings.encryptionAlgorithm);
		}
		catch (NoSuchAlgorithmException e)
		{
			Services.logger.logStream.println("No support for RSA!");
			Services.logger.logStream.println("Quitting.");
			e.printStackTrace(Services.logger.logStream);
			Main.quit();
			return;
		}
		// should be in settings
		int numKeys = 3;
		
		while (keys.size() < numKeys)
		{
			keys.add(generator.generateKeyPair());
		}
	}

	void write()
	{
		
	}
	
	void read()
	{
		
	}

	public String[] getKeys()
	{
		return new String[0];
	}
}
