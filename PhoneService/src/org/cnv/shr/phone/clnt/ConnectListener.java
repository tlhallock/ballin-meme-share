package org.cnv.shr.phone.clnt;

import org.cnv.shr.phone.cmn.PhoneLine;

public interface ConnectListener
{
	public void onConnect(PhoneLine line);
	public void failed();
}
