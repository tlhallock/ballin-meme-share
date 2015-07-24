package org.cnv.shr.cnctn;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.TrackObjectUtils;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;

public class HandshakeClient extends HandShake
{

	public static HandShakeResults initiateHandShake(HandShakeParams params)
	{
		HandShakeResults results = new HandShakeResults();
		try (Socket socket           = new Socket(params.ip, params.port);
				 JsonParser parser       = TrackObjectUtils.createParser(   new SnappyFramedInputStream(socket.getInputStream(), true));
				 JsonGenerator generator = TrackObjectUtils.createGenerator(new SnappyFramedOutputStream(socket.getOutputStream()));)
		{
			generator.writeStartArray();
			
			results.localKey = Services.keyManager.getPublicKey();
			sendInfo(generator, results.localKey);
			RemoteInfo remoteKey = readInfo(parser);
			authenticateToRemote(generator, parser);
			if (isAuthenticated(...generator{
				
			}
			
			if (!authenticateTheRemote(generator, parser, params.identifier))
			{
				return;
			}
			
			generator.writeEnd();
		}
		catch (UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
