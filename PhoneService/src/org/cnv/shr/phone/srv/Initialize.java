package org.cnv.shr.phone.srv;

import java.io.IOException;

import org.cnv.shr.phone.cmn.Services;



public class Initialize
{
	public static void main(String[] args) throws IOException
	{
		ServerSettings settings = new ServerSettings();
		
		VoiceMailManager manager = new VoiceMailManager(settings.voiceRootMailPath);
		
		PhoneProvider isp = new PhoneProvider(manager, settings.connectionPortBegin, settings.connectionPortEnd);
		
		for (int port = settings.metaPortBegin; port < settings.metaPortEnd; port++)
		{
			Services.executor.execute(new Operator(isp, manager, port));
		}
	}
}
