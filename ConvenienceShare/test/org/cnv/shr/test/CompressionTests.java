package org.cnv.shr.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingDeque;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;

import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.CompressionStreams2;
import org.cnv.shr.util.CountingInputStream;
import org.cnv.shr.util.CountingOutputStream;
import org.cnv.shr.util.PausableInputStream2;
import org.cnv.shr.util.PausableOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class CompressionTests
{
	
	// TODO: this class...
	@Test
	public void testCompress() throws IOException
	{
		try (ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				 PausableOutputStream output = new PausableOutputStream(bOut))
		{
			try (OutputStream zipOutputStream = CompressionStreams2.newCompressedOutputStream(output);)
			{
				zipOutputStream.write("here is a string.".getBytes());
				zipOutputStream.flush();
				zipOutputStream.write("here is another string.".getBytes());
				zipOutputStream.flush();
				zipOutputStream.flush();
				zipOutputStream.write("here is the last string.".getBytes());
				zipOutputStream.flush();
			}
			
			output.write("This one is not compressed.".getBytes());
			output.flush();

			try (ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
					 PausableInputStream2 input = new PausableInputStream2(bIn);)
			{
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(CompressionStreams2.newCompressedInputStream(input))))
				{
					String line;
					while ((line = reader.readLine()) != null)
					{
						System.out.println(line);
					}
				}

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(input)))
				{
					String line;
					while ((line = reader.readLine()) != null)
					{
						System.out.println(line);
					}
				}
			}
		}
	}
	
	@Test
	public void simpleTest() throws Exception
	{
		byte[] bytes = "here is some text \nto be compressed.\n".getBytes();
		LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<Integer>();
		
		new Thread() {
			public void run()
			{
				try (InputStream input2 = new InputStream()
				{
					boolean done;
					@Override
					public int read() throws IOException
					{
						if (done)
						{
							return -1;
						}
						try
						{
							Integer takeFirst = queue.takeFirst();
							if (takeFirst < 0)
							{
								done = true;
							}
							return takeFirst;
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
							return -1;
						}
					}

					public int available()
					{
						if (done)
						{
							return 0;
						}
						return queue.size();
					}
				};)
				{
					int ndx = 0;
					try (InputStream input = CompressionStreams2.newCompressedInputStream(new PausableInputStream2(input2)))
					{
						int read;
						while ((read = input.read()) >= 0)
						{
							Assert.assertEquals(bytes[ndx++], read);
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}.start();
		
		try (OutputStream outputStream = new OutputStream()
				{
					@Override
					public void write(int b) throws IOException
					{
						queue.offerLast(b & 0xff);
					}
					public void close()
					{
						queue.offerLast(-1);
					}
				};)
		{
			try (OutputStream output = CompressionStreams2.newCompressedOutputStream(new PausableOutputStream(outputStream));)
			{
				output.write(bytes);
			}
			outputStream.write(1);
			outputStream.write(2);
			outputStream.write(3);
			outputStream.write(4);
		}
	}
	
	
	// Test json bug....

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
				JsonParser    parser    = TrackObjectUtils.createParser   (new CountingInputStream(accept.getInputStream()), true);)
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
					JsonParser    parser    = TrackObjectUtils.createParser   (new CountingInputStream(accept.getInputStream()), true);)
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
	
	@Test
	public void testFlush() throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputStream cout = CompressionStreams2.newCompressedOutputStream(new PausableOutputStream(output));
		byte[] bytes = "1".getBytes();
		cout.write(bytes);
		cout.flush();
		InputStream newCompressedInputStream = CompressionStreams2.newCompressedInputStream(new PausableInputStream2(new ByteArrayInputStream(output.toByteArray())));
		byte[] bSoFar = new byte[bytes.length];
		int soFar = 0;
		while (soFar < bSoFar.length)
		{
			int read = newCompressedInputStream.read(bSoFar, soFar, bSoFar.length - soFar);
			if (read < 0)
			{
				throw new IOException("Did not flush.");
			}
			soFar += read;
		}
		Assert.assertArrayEquals(bytes, bSoFar);
		System.out.println(new String(bSoFar));
	}
}
