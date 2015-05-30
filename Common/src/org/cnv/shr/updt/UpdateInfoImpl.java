package org.cnv.shr.updt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.logging.Level;

import javax.crypto.NoSuchPaddingException;

import org.cnv.shr.util.KeysService;
import org.cnv.shr.util.LogWrapper;

public class UpdateInfoImpl implements UpdateInfo
{
	private File propsFile;
	private File keysFile;
	
	private long timeStamp;
	private Properties props;
	private KeysService keys;
	public static final String KEYS_TXT = "keys.txt";
	public static final String INFO_PROPS = "info.props";
	
	public UpdateInfoImpl(File propsFile, File keysFile) throws CertificateEncodingException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeySpecException, ClassNotFoundException, IOException
	{
		this.propsFile = propsFile;
		this.keysFile  = keysFile;
		
		props = new Properties();
		keys = new KeysService();
		keys.readKeys(keysFile, -1);
	}
	
	
	@Override
	public String getIp()
	{
		return props.getProperty("ip");
	}

	@Override
	public int getPort()
	{
		return Integer.parseInt(props.getProperty("port"));
	}

	@Override
	public PrivateKey getPrivateKey(PublicKey usedKey)
	{
		return keys.getPrivateKey(usedKey);
	}

	@Override
	public PublicKey getLatestPublicKey()
	{
		return keys.getPublicKey();
	}
	
	private synchronized void checkTime()
	{
		long fsTime = Math.max(propsFile.lastModified(), keysFile.lastModified());
		if (fsTime < timeStamp)
		{
			return;
		}
		read(fsTime);
	}

	private void read(long ts)
	{
		try (InputStream input = new FileInputStream(propsFile))
		{
			Properties newProps = new Properties();
			newProps.load(input);
			props = newProps;
			timeStamp = ts;
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read update manager info.", e);
		}
		try
		{
			keys.readKeys(keysFile, -1);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read keys file.", e);
		}
	}
	
	public static void write(File propsFile, String ip, int port) throws FileNotFoundException, IOException
	{
		Properties props = new Properties();
		props.setProperty("ip", ip);
		props.setProperty("port", String.valueOf(port));
		
		try (FileOutputStream fileOutputStream = new FileOutputStream(propsFile);)
		{
			props.store(fileOutputStream, null);
		}
	}
}
