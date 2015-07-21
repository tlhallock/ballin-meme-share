package org.cnv.shr.phone.clnt;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.msg.PhoneRing;
import org.cnv.shr.phone.msg.VoiceMail;

public abstract class DialListener
{
	public abstract void onRing(PhoneRing ring, PhoneLine line);
	public abstract void onVoiceMail(VoiceMail mail);
	public abstract void onFail(int code, String reason);
}
