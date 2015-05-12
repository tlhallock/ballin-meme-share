package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.cnv.shr.util.Misc;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class SimpleTest
{

	public static void main(String[] args) throws Exception
	{
//		byte[] naunce = Misc.getBytes(50);
//		System.out.println(Misc.format(naunce));
//		System.out.println(Misc.format(Misc.format(Misc.format(naunce))));
		
		KeyManager keyManager1 = new KeyManager(new File("./keys.txt"));
		keyManager1.writeKeys();
		keyManager1.readKeys();
		
		KeyManager keyManager2 = new KeyManager(new File("./keys.txt"));
		keyManager2.writeKeys();
		keyManager2.readKeys();
		
		String inputString = "This is some text that I hope makes it across.";
		
		InputStream input = new ByteInputStream(inputString.getBytes(), 0, inputString.getBytes().length);
		ByteOutputStream outputCopy = new ByteOutputStream();
		OutputStream output = outputCopy;
		
		// encrypt public2
		Cipher cipher2 = Cipher.getInstance("RSA", "FlexiCore"); cipher2.init(Cipher.ENCRYPT_MODE, keyManager2.getPublicKey());
		input = new CipherInputStream(new GZIPInputStream(input), cipher2);
		
		
		input.close();
		
		
		// decrypt private2
		Cipher cipher3 = Cipher.getInstance("RSA", "FlexiCore"); cipher3.init(Cipher.DECRYPT_MODE, keyManager2.getPrivateKey());
		output = new CipherOutputStream(new GZIPOutputStream(output), cipher3);


		byte[] buffer = new byte[1024];
		int offset;
		
		while ((offset = input.read(buffer, 0, buffer.length)) >= 0)
		{
			output.write(buffer, 0, offset);
		}
		
		String outputString = new String((outputCopy).getBytes());
		
		
		System.out.println(inputString);
		System.out.println(outputString);
	}
}
