package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.OutputByteWriter;
import org.cnv.shr.util.ProcessInfo;

public class UpdateManager extends TimerTask
{
	private String currentVersion;
	
	private String ip;
	private int port;
	private PublicKey pKey;
	
	public UpdateManager(String currentVersion)
	{
		this.currentVersion = currentVersion;
	}

	@Override
	public void run()
	{
		checkForUpdates();
	}

	public synchronized void checkForUpdates()
	{
		if (pKey == null)
		{
			LogWrapper.getLogger().warning("No way to update code! (No public key)");
			return;
		}
		if (ip == null || port <= 0)
		{
			LogWrapper.getLogger().info("No way to update code! (No ip/port)");
			return;
		}

		try (Socket socket = new Socket(ip, port); 
				InputStream inputStream = socket.getInputStream(); 
				OutputStream outputStream = socket.getOutputStream();)
		{
			OutputByteWriter writer = new OutputByteWriter(outputStream);
			ByteReader byteReader = new ByteReader(inputStream);

			byte[] naunce = Misc.createNaunce(Services.settings.minNaunce.get());
			byte[] encrypted = Services.keyManager.encrypt(pKey, naunce);
			
			writer.append(encrypted);
			byte[] decrypted = byteReader.readVarByteArray();
			if (!Arrays.equals(naunce, decrypted))
			{
				LogWrapper.getLogger().info("Update server failed authentication.");
				return;
			}
			String newVersionString = byteReader.readString();
			if (!versionIsNewer(newVersionString))
			{
				LogWrapper.getLogger().info("We already have the latest version.");
				return;
			}

			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, 
					"A new version of code has been found. Would you like to update now?", 
					"Update ConvenienceShare", 
					JOptionPane.YES_NO_OPTION))
			{
				writer.append(true);
				update(inputStream);
			}
			else
			{
				writer.append(false);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to open connection to update server", e);
		}
	}
	
	
	private boolean versionIsNewer(String newVersionString)
	{
		String[] current = currentVersion.split(".");
		String[] newer = currentVersion.split(".");
		int versionLength = Math.min(current.length, newer.length);
		for (int i = 0; i < versionLength; i++)
		{
			int newerNugget = Integer.parseInt(newer[i]);
			int currentNugget = Integer.parseInt(current[i]);
			
			if (newerNugget > currentNugget)
			{
				return true;
			}
			else if (newerNugget < currentNugget)
			{
				return false;
			}
		}
		
		return current.length > newer.length;
	}

	private void update(InputStream input)
	{
		String jarPath = ProcessInfo.getJarPath(Main.class);
		File downloadFile = new File(jarPath + ".new");
		
		try (Socket socket = new Socket(ip, port);
				InputStream inputStream = socket.getInputStream();
				FileOutputStream outputStream = new FileOutputStream(downloadFile))
		{
			byte[] buffer = new byte[1024];
			int nread;
			while ((nread = inputStream.read(buffer, 0, buffer.length)) >= 0)
			{
				outputStream.write(buffer, 0, nread);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to download new code.", e);
			return;
		}
		
		try
		{
			Files.copy(Paths.get(jarPath), Paths.get(jarPath + ".bak"));
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to backup current code.", e);
			downloadFile.delete();
			return;
		}

		try
		{
			Files.move(Paths.get(downloadFile.getAbsolutePath()), Paths.get(jarPath));
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to replace current code.", e);
			downloadFile.delete();
			return;
		}

		JOptionPane.showMessageDialog(null, 
				"Code update is complete. ConvenienceShare will now restart.",
				"Code update.",
				JOptionPane.INFORMATION_MESSAGE);
		Main.restart();
	}

	public synchronized void updateInfo(String ip, int port, PublicKey pKey)
	{
		this.ip = ip;
		this.port = port;
		this.pKey = pKey;
		write();
	}

	public PublicKey getPublicKey()
	{
		return pKey;
	}

	public synchronized void read()
	{
		System.out.println(Services.settings.codeUpdateKey.getPath());
		try (InputStream input = Files.newInputStream(Services.settings.codeUpdateKey.getPath()))
		{
			ByteReader byteReader = new ByteReader(input);
			ip = byteReader.readString();
			port = byteReader.readInt();
			pKey = byteReader.readPublicKey();
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read update info.", e);
			
			ip = "";
			port = -1;
			pKey = null;
			write();
		}
	}
	
	public synchronized void write()
	{
		writeTo(Services.settings.codeUpdateKey.get());
	}

	private void writeTo(File file)
	{
		try (FileOutputStream output = new FileOutputStream(file))
		{
			new OutputByteWriter(output)
				.append(ip)
				.append(port)
				.append(pKey);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save update info", e);
		}
	}
}
