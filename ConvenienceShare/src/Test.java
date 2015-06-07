import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.CountingOutputStream;


public class Test
{

	public static void main(String[] args) throws IOException
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				try
				{
					otherThread();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}}).start();
		
		try (ServerSocket server = new ServerSocket(9999);
				Socket accept = server.accept();
				JsonGenerator generator = TrackObjectUtils.createGenerator(new CountingOutputStream(accept.getOutputStream()));
				JsonParser    parser    = TrackObjectUtils.createParser   (new CountingInputStream(accept.getInputStream()));)
		{
			generator.writeStartArray();
			generator.write("foobar");
			generator.flush();
			
			String string = null;
			parser.next(); // start
			parser.next(); // value
			string = parser.getString();
			
			generator.writeEnd();
			generator.flush();
			
			parser.next(); // end

			System.out.println("Found string " + string);
		}
	}

	private static void otherThread() throws UnknownHostException, IOException
	{
			try (Socket accept = new Socket("127.0.0.1", 9999);
					JsonGenerator generator = TrackObjectUtils.createGenerator(new CountingOutputStream(accept.getOutputStream()));
					JsonParser    parser    = TrackObjectUtils.createParser   (new CountingInputStream(accept.getInputStream()));)
			{
				generator.writeStartArray();
				generator.write("foobar");
				generator.flush();
				
				String string = null;
				parser.next(); // start
				parser.next(); // value
				string = parser.getString();
				
				generator.writeEnd();
				generator.flush();
				
				parser.next(); // end

				System.out.println("Found string " + string);
			}
	}
}
