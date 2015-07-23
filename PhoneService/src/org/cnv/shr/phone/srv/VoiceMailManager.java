package org.cnv.shr.phone.srv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.Predicate;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.cmn.PhoneNumber;
import org.cnv.shr.phone.cmn.PhoneNumberWildCard;
import org.cnv.shr.phone.msg.VoiceMail;

public class VoiceMailManager
{
	private LinkedList<VoiceMailEntry> entries = new LinkedList<>();
	
	private long voiceMailHold;
	
	public VoiceMailManager(ServerSettings settings)
	{
		voiceMailHold = settings.MAXIMUM_VOICE_MAIL_TIME;
		// i suppose read the old ones? from settings.voiceRootMailPath
	}
	
	public void sendVoiceMails(PhoneLine phoneLine) throws IOException
	{
		PhoneNumber number = phoneLine.getInfo().getNumber();
		LinkedList<VoiceMailEntry> mails = new LinkedList<>();
		
		synchronized (entries)
		{
			long now = System.currentTimeMillis();
			entries.removeIf(new Predicate<VoiceMailEntry>()
			{
				@Override
				public boolean test(VoiceMailEntry t)
				{
					boolean expired = t.added + voiceMailHold < now;
					
					if (expired && t.voiceMail.hasData())
					{
						// delete voice mail data...
					}
					
					return expired; 
				}
			});
			
			for (VoiceMailEntry entry : entries)
			{
				if (entry.pattern.matches(number))
				{
					mails.add(entry);
				}
			}
		}
		
		for (VoiceMailEntry entry : mails)
		{
			phoneLine.sendMessage(entry.createServeVoiceMail());
		}
	}

	public void newVoiceMail(Object caller, VoiceMail mail)
	{
//		synchronized (entries)
//		{
//			entries.add(new VoiceMailEntry())
//		}
	}

	private static class VoiceMailEntry
	{
		// I guess this needs to be its own class. and jsonable.
		private long added;
		private PhoneNumberWildCard pattern;
		private Path voiceMailData;
		private VoiceMail voiceMail;
		private Object caller;
		
		public VoiceMailEntry(PhoneNumberWildCard pattern, Path voiceMailData, VoiceMail voiceMail)
		{
			this.added = System.currentTimeMillis();
			this.pattern = pattern;
			this.voiceMailData = voiceMailData;
			this.voiceMail = voiceMail;
		}
		
		public VoiceMail createServeVoiceMail() throws IOException
		{
			if (voiceMail.hasData())
			{
				return new VoiceMail(Files.newInputStream(voiceMail.getDataPath()));
			}
			else
			{
				// set the caller as the destination...
//				return new VoiceMail()
				return null;
			}
		}
	}

	
}
