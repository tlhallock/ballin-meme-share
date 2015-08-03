import java.lang.Thread.UncaughtExceptionHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.json.stream.JsonParser;

import org.cnv.shr.cnctn.Communication;
import org.cnv.shr.cnctn.KeyInfo;
import org.cnv.shr.msg.MessageReader;
import org.cnv.shr.msg.Wait;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.junit.Assert;
import org.junit.Test;

public class TestCommunicationStreams
{
	private static final int TEST_PORT = 7006;
	private static MessageReader reader = new MessageReader();
	
	private static void stream1(KeyInfo in, KeyInfo out, String[] strings)
	{
		try (ServerSocket serverSocket = new ServerSocket(TEST_PORT);
				 Socket socket = serverSocket.accept();
				Communication communication = new Communication(
						socket,
						in,
						out,
						"foobar",
						"no reason",
						true);)
		{
			int count = 0;
			for (String toWrite : strings)
			{
				communication.send(new Wait());

				System.out.println(count++ + ":");
				System.out.println("Writing " + toWrite);
				
				byte[] bytes = toWrite.getBytes();
				
				communication.getGenerator().writeStartObject("key");
				communication.getGenerator().write("numBytes", bytes.length);
				communication.getGenerator().writeEnd();
				communication.flush();
				
				communication.beginWriteRaw();
				communication.getOutput().write(bytes);
				communication.endWriteRaw();
	
				communication.send(new Wait());
				
				if (Math.random() < .333333)
				{
					Thread.sleep(20);
				}
			}

			communication.send(new Wait());

			communication.getGenerator().writeStartObject("key");
			communication.getGenerator().write("numBytes", -1);
			communication.getGenerator().writeEnd();
			communication.flush();
		}
		catch (Throwable e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, null, e);
			Assert.fail("dare was an exception");
		}
	}
	private static void stream2(KeyInfo in, KeyInfo out, LinkedList<String> collect) 
	{
		try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
				 Communication communication = new Communication(
						socket,
						in,
						out,
						"foobar",
						"no reason",
						true);)
		{
			int count = 0;
			for (;;)
			{
				++count;
				reader.readMsg(communication.getParser(), "debug source");
				
				JsonParser.Event e;
				e = communication.getParser().next(); Assert.assertEquals("wrong 1 on " + count, e, JsonParser.Event.KEY_NAME    );
				e = communication.getParser().next(); Assert.assertEquals("wrong 2 on " + count, e, JsonParser.Event.START_OBJECT);
				e = communication.getParser().next(); Assert.assertEquals("wrong 3 on " + count, e, JsonParser.Event.KEY_NAME    );
				e = communication.getParser().next(); Assert.assertEquals("wrong 4 on " + count, e, JsonParser.Event.VALUE_NUMBER);
				int numbytes = communication.getParser().getInt();                 
				e = communication.getParser().next(); Assert.assertEquals("wrong 5 on " + count, e, JsonParser.Event.END_OBJECT  );
				
				if (numbytes < 0)
				{
					break;
				}
				
				byte[] someBytes = new byte[numbytes];
				
				communication.beginReadRaw();
				int offset = 0;
				while (offset < someBytes.length)
				{
					offset += communication.getIn().read(someBytes, offset, someBytes.length - offset);
				}
				communication.endReadRaw();
				
				String string = new String(someBytes);
				System.out.println("Read some bytes: " + string);
				collect.add(string);
				reader.readMsg(communication.getParser(), "debug source");
			}
		}
		catch (Throwable e)
		{
			LogWrapper.getLogger().log(Level.SEVERE, null, e);
			Assert.fail("dere was an exception");
		}
	}
	
	
	private static String[] createArray(int num)
	{
		Random random = new Random();
		String[] returnValue = new String[num];
		for (int i = 0; i < num; i++)
		{
			returnValue[i] = Misc.getRandomString(random.nextInt(2 * 8192));
		}
		return returnValue;
	}

	@Test
	public void simpleTest() throws NoSuchAlgorithmException, InterruptedException
	{
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Uncaught exception in thread " + t.getName() + ":" + t.getId(), e);
				Assert.fail();
			}
		});
		
		KeyInfo out1 = new KeyInfo();
		KeyInfo in1 = new KeyInfo();
		
		String[] strings = createArray(50);

		CountDownLatch latch = new CountDownLatch(2);
		
		LinkedList<String> returnValue = new LinkedList<>();

		new Thread(() -> {
			stream1(in1, out1, strings);
			latch.countDown();
		}).start();
		Thread.sleep(1000);
		new Thread(() -> {
			stream2(out1, in1, returnValue);
			latch.countDown();
		}).start();
		
		latch.await();
		
		String[] array = returnValue.toArray(new String[0]);
		Assert.assertArrayEquals(strings, array);
	}
}
