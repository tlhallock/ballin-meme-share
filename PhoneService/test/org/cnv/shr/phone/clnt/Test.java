package org.cnv.shr.phone.clnt;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.cnv.shr.phone.cmn.PhoneLine;
import org.cnv.shr.phone.msg.PhoneRing;
import org.cnv.shr.phone.msg.VoiceMail;

public class Test
{
	public static void main(String[] args)
	{
		if (true)
		{
			Dialer runDialer1 = runDialer("foobar1", Paths.get("persistance1.json"));
		}
		else
		{
			Dialer runDialer2 = runDialer("foobar2", Paths.get("persistance2.json"));
		}
	}
	
	public static Dialer runDialer(String name, Path p)
	{
		DialerPersistance persistance = new DialerPersistance(p);
		persistance.load();
		persistance.params.voiceMailDirectory = Paths.get("voicemails");
		persistance.params.ident = name;
		persistance.save();
		OperatorInfo info = new OperatorInfo("127.0.0.1", 7020, 7030);
		persistance.add(info);
		
		Dialer dialer = new Dialer(info, new DialListener() {
			@Override
			public void onRing(PhoneRing ring, PhoneLine line)
			{
				System.out.println("Connection opened!");
			}

			@Override
			public void onVoiceMail(VoiceMail mail)
			{
				System.out.println("Received voice mail: " + mail);
			}

			@Override
			public void onFail(int code, String reason)
			{
				System.out.println("Connection closed: " + code + ": " + reason);
			}}, persistance);
		
		dialer.start();
		
		return dialer;
	}
}
