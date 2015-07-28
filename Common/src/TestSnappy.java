import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Random;

import org.cnv.shr.util.TransferStream;
import org.iq80.snappy.SnappyFramedInputStream;
import org.iq80.snappy.SnappyFramedOutputStream;
import org.junit.Assert;
import org.junit.Test;


public class TestSnappy
{
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
//		mostSimpleTest();
		TransferStream transferStream = new TransferStream();
		
		new Thread()
		{
			public void run()
			{
				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
						new SnappyFramedInputStream(
								transferStream.getInput()
								, true)
						));)
				{
					String line;
					while ((line = bufferedReader.readLine()) != null)
					{
						System.out.println(line);
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}.start();
		
		try (OutputStream snappyOutputStream = 
				new SnappyFramedOutputStream(
						transferStream.getOutput()
						)
		;)
		{
			for (int i = 0; i < 5; i++)
			{
				snappyOutputStream.write(("writing " + i + "\n").getBytes());
				snappyOutputStream.flush();
				Thread.sleep(1000);
			}
		}
	}
	
	
	
	private static void mostSimpleTest() throws IOException
	{
		byte[] testBytes = new byte[8192];
		new Random().nextBytes(testBytes);
		
		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
		try (SnappyFramedOutputStream snappyOutputStream = new SnappyFramedOutputStream(compressedStream);)
		{
			snappyOutputStream.write(testBytes);
		}
		ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
		try (SnappyFramedInputStream snappyOutputStream = new SnappyFramedInputStream(new ByteArrayInputStream(compressedStream.toByteArray()), false);)
		{
			int read;
			byte[] buffer = new byte[50];
			while ((read = snappyOutputStream.read(buffer, 0, buffer.length)) > 0)
			{
				uncompressedStream.write(buffer, 0, read);
			}
		}
		
		Assert.assertArrayEquals(testBytes, uncompressedStream.toByteArray());
		System.out.println("Test passed!!!");
	}



	@Test
	public void testTransfer() throws IOException
	{
		byte[] expected = "here is some bytes".getBytes();
		
		TransferStream transferStream = new TransferStream();
		
		try (OutputStream output = transferStream.getOutput();)
		{
			output.write(expected);
		}
		
		byte[] actual = new byte[expected.length];
		try (InputStream input = transferStream.getInput();)
		{
			int offset = 0;
			for (;;)
			{
				int amountToRead = actual.length - offset;
				if (amountToRead < 0)
				{
					Assert.fail();
				}
				if (amountToRead == 0)
				{
					Assert.assertEquals(-1, input.read());
					break;
				}
				int read = input.read(actual, offset, amountToRead);
				if (read < 0)
				{
					Assert.fail();
				}
				offset += read;
			}
		}
		Assert.assertArrayEquals(expected, actual);
	}
}
