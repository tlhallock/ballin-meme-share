package org.cnv.shr.phone.msg;

import org.cnv.shr.phone.cmn.PhoneLine;

public interface MsgHandler
{
	// client messages
	public void onRing        (PhoneRing ring);
	public void onError       (Hangup hangup);
	public void onHeartBeatAwk(HeartBeatResponse res);
	
	// server messages
	public void onHeartBeatReq(PhoneLine line, HeartBeatRequest req);
	public void onDial        (PhoneLine line, Dial dial) throws InterruptedException;
	public void onClientInfo  (PhoneLine line, ClientInfo clientInfo);
	

	// both
	public void onVoicemail   (VoiceMail mail);
	public void onNoMore      ();
}
