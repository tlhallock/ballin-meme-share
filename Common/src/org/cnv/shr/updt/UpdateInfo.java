package org.cnv.shr.updt;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface UpdateInfo
{
	public String getIp();
	public int getPort();
	public PrivateKey getPrivateKey(PublicKey usedKey);
	public PublicKey getLatestPublicKey();
	
	public static final int DEFAULT_UPDATE_PORT = 7005;
}
