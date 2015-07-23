package org.cnv.shr.phone.msg;

import org.cnv.shr.phone.cmn.PhoneLine;

public interface MsgHandler
{
	// client messages
	public void onRing        (PhoneRing ring);
	public void onError       (Hangup hangup);
	public void onHeartBeatAwk(HeartBeatResponse res);
	public void onMissedCall  (PhoneLine line, DialFail dialFail);
	
	// server messages
	public void onHeartBeatReq(PhoneLine line, HeartBeatRequest req);
	public void onDial        (PhoneLine line, Dial dial) throws InterruptedException;
	public void onClientInfo  (PhoneLine line, ClientInfo clientInfo);
	

	// both
	public void onVoicemail   (PhoneLine phoneLine, VoiceMail mail);
	public void onNoMore      ();
}
