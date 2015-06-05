package org.cnv.shr.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Scanner;

import de.flexiprovider.common.math.FlexiBigInt;
import de.flexiprovider.core.rsa.RSAPrivateCrtKey;
import de.flexiprovider.core.rsa.RSAPublicKey;

public class KeyPairObject
{
	de.flexiprovider.core.rsa.RSAPublicKey publicKey;
	de.flexiprovider.core.rsa.RSAPrivateCrtKey privateKey;
	long timeStamp;
	
	public KeyPairObject(KeyPairGenerator kpg)
	{
		KeyPair pair = kpg.generateKeyPair();
		privateKey = (RSAPrivateCrtKey) pair.getPrivate();
		publicKey = (RSAPublicKey) pair.getPublic();
		timeStamp = System.currentTimeMillis();
	}
	
	public KeyPairObject(Scanner scanner) throws InvalidKeySpecException
	{
		privateKey = deSerializePrivateKey(scanner);
		publicKey = deSerializePublicKey(scanner);
		timeStamp  = scanner.nextLong();
	}
	
	public void write(PrintStream output) throws IOException
	{
		serialize(privateKey, output);
		serialize(publicKey, output);
		output.print(timeStamp);
	}
	
	public String hash()
	{
		return hashObject(publicKey);
	}

	static String hashObject(PublicKey publicKey)
	{
		return Misc.format(publicKey.getEncoded());
	}

    public String getTime() {
        return new Date(timeStamp).toString();
    }


    public RSAPublicKey getPublicKey() {
        return publicKey;
    }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// I wish these serializable objects were serializable.
	public static void serialize(RSAPublicKey publicKey, PrintStream ps)
	{
		ps.print(Misc.format( publicKey.getN().toByteArray()) + " ");
		ps.print(Misc.format( publicKey.getE().toByteArray()) + " ");
	}
	
	public static RSAPublicKey deSerializePublicKey(Scanner scanner)
	{
		FlexiBigInt publn         = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt puble         = new FlexiBigInt(Misc.format(scanner.next()));
		
		return new de.flexiprovider.core.rsa.RSAPublicKey(publn, puble);
	}

	public static void serialize(RSAPrivateCrtKey privateKey, PrintStream ps)
	{
		ps.print(Misc.format(privateKey.getN()       .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getE()       .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getD()       .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getP()       .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getQ()       .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getDp()      .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getDq()      .toByteArray()) + " ");
		ps.print(Misc.format(privateKey.getCRTCoeff().toByteArray()) + " ");
		
	}
	public static RSAPrivateCrtKey deSerializePrivateKey(Scanner scanner)
	{
		FlexiBigInt privn         = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt prive         = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt privd         = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt privp         = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt privq         = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt privdP        = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt privdQ        = new FlexiBigInt(Misc.format(scanner.next()));
		FlexiBigInt privcrtCoeff  = new FlexiBigInt(Misc.format(scanner.next()));
		
		return new de.flexiprovider.core.rsa.RSAPrivateCrtKey(
				privn       ,
				prive       ,
				privd       ,
				privp       ,
				privq       ,
				privdP      ,
				privdQ      ,
				privcrtCoeff);
	}
	
	public static String serialize(RSAPublicKey publicKey)
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(byteArrayOutputStream);)
		{
			serialize(publicKey, ps);
		}
		return new String(byteArrayOutputStream.toByteArray());
	}
	
	public static RSAPublicKey deSerializePublicKey(String str)
	{
		try (Scanner s = new Scanner(str))
		{
			return deSerializePublicKey(s);
		}
	}

	public static String serialize(RSAPrivateCrtKey privateKey)
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(byteArrayOutputStream);)
		{
			serialize(privateKey, ps);
		}
		return new String(byteArrayOutputStream.toByteArray());
	}
	
	public static RSAPrivateCrtKey deSerializePrivateKey(String str)
	{
		try (Scanner s = new Scanner(str))
		{
			return deSerializePrivateKey(s);
		}
	}
}
