package org.cnv.shr.dmn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.updt.UpdateInfo;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.OutputByteWriter;
import org.cnv.shr.util.ProcessInfo;

/**
 * This file could use some refactoring...
 */

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
		Services.userThreads.execute(new Runnable()
		{
			@Override
			public void run()
			{
				checkForUpdates(null, true);
			}
		});
	}

	public String getUpdateServerUrl()
	{
		return ip;
	}

	public int getUpdateServerPort()
	{
		return port;
	}

	public boolean hasKey()
	{
		return pKey != null;
	}

	public void setAddress(String ip, int port)
	{
		this.ip = ip;
		this.port = port;
	}

	public synchronized void checkForUpdates(JFrame origin, boolean authenticate)
	{
		if (pKey == null && authenticate)
		{
			LogWrapper.getLogger().warning("No way to update code! (No public key)");
			return;
		}
		if (ip == null)
		{
			LogWrapper.getLogger().info("No way to update code! (No ip/port)");
			return;
		}
		
		if (port <= 0)
		{
			port = UpdateInfo.DEFAULT_UPDATE_PORT;
		}

		try (Socket socket = new Socket(ip, port);
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();)
		{
			OutputByteWriter writer = new OutputByteWriter(outputStream);
			ByteReader byteReader = new ByteReader(inputStream);

			writer.append(authenticate);
			if (authenticate)
			{
				byte[] naunce = Misc.createNaunce(Services.settings.minNaunce.get());
				byte[] encrypted = Services.keyManager.encrypt(pKey, naunce);

				writer.appendVarByteArray(encrypted);
				byte[] decrypted = byteReader.readVarByteArray();
				if (!Arrays.equals(naunce, decrypted))
				{
					LogWrapper.getLogger().info("Update server failed authentication.");
					return;
				}
			}

			String serverVersionString = byteReader.readString();
			if (!versionIsNewer(serverVersionString, currentVersion))
			{
				LogWrapper.getLogger().info("We already have the latest version.");
				writer.append(false);
				
				// here we could also check the version on file...
				return;
			}

			update(inputStream, writer, serverVersionString, origin);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to open connection to update server", e);
			
			Path currentJar = ProcessInfo.getJarFile(Main.class);
			Path downloadFile = Paths.get(currentJar.toString() + ".new");
			if (shouldUseDownloadedVersion(downloadFile, currentVersion, false))
			{
				LogWrapper.getLogger().info("Found previous code download.");
				completeUpdate(currentJar, downloadFile, origin);
			}
		}
	}
	
	private boolean confirmUpgrade(JFrame origin)
	{
			UserInputWait wait = new UserInputWait();
			waitForInput(wait);
			boolean proceed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(origin,
					"A new version of code has been found. Would you like to update now?",
					"Update ConvenienceShare",
					JOptionPane.YES_NO_OPTION);
			wait.userInput = true;
			return proceed || wait.interrupted;
	}

	private boolean versionIsNewer(String serverVersion, String currentVersion)
	{
		String[] current = currentVersion.split("\\.");
		String[] newer = serverVersion.split("\\.");
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

	private void update(InputStream input, OutputByteWriter writer, String version, JFrame origin)
	{
		Path currentJar = ProcessInfo.getJarFile(Main.class);
		Path downloadFile = Paths.get(currentJar.toString() + ".new");
		if (shouldUseDownloadedVersion(downloadFile, version, true))
		{
			try
			{
				// Notify done...
				writer.append(false);
			}
			catch (IOException e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Server quit on us. No big thing.", e1);
			}
			LogWrapper.getLogger().info("Found previous code download.");
			completeUpdate(currentJar, downloadFile, origin);
			return;
		}
		
		try
		{
			writer.append(true);
			Files.copy(input, downloadFile, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to download new code.", e);
			try
			{
				Files.delete(downloadFile);
			}
			catch (IOException e1)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to delete incomplete download.", e);
			}
			return;
		}

		try
		{
			// Notify done...
			writer.append(false);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.INFO, "Server quit on us. No big thing.", e1);
		}
		
		completeUpdate(currentJar, downloadFile, origin);
	}

	private void completeUpdate(Path currentJar, Path downloadFile, JFrame origin)
	{
		if (!confirmUpgrade(origin))
		{
			return;
		}
		
		try
		{
			if (Files.exists(currentJar))
			{
				Files.copy(currentJar, Paths.get(currentJar.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to backup current code.", e);
			return;
		}

		try
		{
			Files.copy(downloadFile, currentJar, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to replace current code.", e);
			return;
		}

		restart(origin);
	}

	private void restart(JFrame origin)
	{
		UserInputWait wait = new UserInputWait();
		waitForInput(wait);
		JOptionPane.showMessageDialog(origin, 
				"Code update is complete. ConvenienceShare will now restart.", 
				"Code update.", 
				JOptionPane.INFORMATION_MESSAGE);
		wait.userInput = true;
		Main.restart();
	}
	
	private boolean shouldUseDownloadedVersion(Path downloadFile, String requiredVersion, boolean updateOnEqual)
	{
		if (!Files.exists(downloadFile))
		{
			return false;
		}
		String jarVersion = ProcessInfo.getJarVersion(downloadFile);
		if (jarVersion == null)
		{
			return false;
		}
		if (versionIsNewer(jarVersion, requiredVersion))
		{
			return true;
		}
		if (requiredVersion.equals(jarVersion))
		{
			return updateOnEqual;
		}
		return false;
	}

	private void waitForInput(UserInputWait wait)
	{
		Thread t = Thread.currentThread();
		Services.timer.schedule(new TimerTask()
		{
			@Override
			public void run()
			{
				if (!wait.userInput)
				{
					LogWrapper.getLogger().log(Level.INFO, "No user input, proceeding");
					wait.interrupted = true;
					t.interrupt();
				}
			}
		}, 10 * 1000);
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

	private static final class UserInputWait
	{
		boolean interrupted;
		boolean userInput;
	}
}
