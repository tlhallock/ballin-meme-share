package org.cnv.shr.phone.srv;

import java.nio.file.Path;

import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.msg.VoiceMail;

public class VoiceMailManager
{
	public VoiceMailManager(Path voiceRootMailPath)
	{
		// TODO Auto-generated constructor stub
	}

	public void newVoiceMail(Object caller, VoiceMail mail)
	{
		
	}

	private static class VoiceMailEntry
	{
		PhoneNumberWildCard pattern;
		Path voiceMail;
	}
}
