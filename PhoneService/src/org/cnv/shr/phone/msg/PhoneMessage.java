package org.cnv.shr.phone.msg;

import org.cnv.shr.phone.cmn.ConnectionParams;
import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.Storable;

public abstract class PhoneMessage implements Storable
{
	protected PhoneMessage(ConnectionParams params) {}
	protected PhoneMessage() {}

	public abstract void perform(PhoneLine line, MsgHandler listener) throws Exception;
	public abstract String getJsonKey();
}
