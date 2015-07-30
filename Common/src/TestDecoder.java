import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import org.cnv.shr.util.OMGJavaDecoder;
import org.cnv.shr.util.TransferStream;


public class TestDecoder
{

	public static void main(String[] args) throws IOException
	{
		Random random = new Random();
		TransferStream transferStream = new TransferStream();
		BufferedReader reader = new BufferedReader(new OMGJavaDecoder(transferStream.getInput(), 32));
		new Thread(() -> {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 10; i++)
			{
				builder.setLength(0);
				builder.append(i).append(":");
				int numChars = random.nextInt(50);
				builder.append(random.nextDouble()).append(" Making string with ").append(numChars).append(":\n");
				for (int j=0;j<numChars;j++)
				{
					builder.append("\u00C3");
				}
				builder.append('\n');
				String str = builder.toString();
				try
				{
					transferStream.getOutput().write(str.getBytes());
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
				try
				{
					Thread.sleep(1000);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try
			{
				transferStream.getOutput().close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}).start();
		
		String line;
		while ((line = reader.readLine()) != null)
		{
			System.out.println(line);
		}
	}

//	private static void ByteBufferTest()
//	{
//		ByteBuffer buffer = ByteBuffer.allocate(1024);
//		for (;;)
//		{
//			
//		}
//	}
}
