
/*                                                                          *
 * Copyright (C) 2015    Trever Hallock                                     *
 *                                                                          *
 * This program is free software; you can redistribute it and/or modify     *
 * it under the terms of the GNU General Public License as published by     *
 * the Free Software Foundation; either version 2 of the License, or        *
 * (at your option) any later version.                                      *
 *                                                                          *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 *                                                                          *
 * You should have received a copy of the GNU General Public License along  *
 * with this program; if not, write to the Free Software Foundation, Inc.,  *
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
 *                                                                          *
 * See LICENSE file at repo head at                                         *
 * https://github.com/tlhallock/ballin-meme-share                           *
 * or after                                                                 *
 * git clone git@github.com:tlhallock/ballin-meme-share.git                 */

package org.cnv.shr.dmn;

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
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.zip.ZipInputStream;

import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cnv.shr.db.h2.bak.DbBackupRestore;
import org.cnv.shr.dmn.mn.Main;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.updt.UpdateInfo;
import org.cnv.shr.util.ByteReader;
import org.cnv.shr.util.LogWrapper;
import org.cnv.shr.util.Misc;
import org.cnv.shr.util.OutputByteWriter;
import org.cnv.shr.util.ProcessInfo;

public class UpdateManager extends TimerTask
{
	private String currentVersion;
	
	// Destination for update
	private Path currentJarPath = ProcessInfo.getJarPath(Main.class);
	// Tmp file to download 
	private Path updatesFile = currentJarPath.resolve("updates.zip");
	// Database backup
	private Path dbBackup = currentJarPath.resolve("dbBackup.json");
	
	private JsonableUpdateInfo info;

	public UpdateManager(String currentVersion)
	{
		this.currentVersion = currentVersion;
		info = new JsonableUpdateInfo();
	}

	public String getUpdateServerUrl()
	{
		return info.getIp();
	}

	public int getUpdateServerPort()
	{
		int port = info.getPort();
		if (port <= 0)
		{
			port = UpdateInfo.DEFAULT_UPDATE_PORT;
		}
		return port;
	}

	public boolean hasKey()
	{
		return info.getKey() != null;
	}

	public void setAddress(String ip, int port)
	{
		info.setAddress(ip, port);
	}

	public synchronized void updateInfo(String ip, int port, PublicKey pKey)
	{
		info.updateInfo(ip, port, pKey);
		write();
	}

	public PublicKey getPublicKey()
	{
		return info.getKey();
	}
	
	
	@Override
	public void run()
	{
		Services.userThreads.execute(() ->
		{
				checkForUpdates(null, true);
		});
	}

	public void checkForUpdates(JFrame origin, boolean authenticate)
	{
		if (!hasKey() && authenticate)
		{
			LogWrapper.getLogger().warning("No way to update code! (No public key)");
			return;
		}
		if (getUpdateServerUrl() == null)
		{
			LogWrapper.getLogger().info("No way to update code! (No ip/port)");
			return;
		}
		
		String versionOnDisk;
		String requiredVersion = currentVersion;
		if (Files.exists(updatesFile) && versionIsNewer(versionOnDisk = ProcessInfo.getJarVersionFromUpdates(updatesFile, Misc.CONVENIENCE_SHARE_JAR), currentVersion))
		{
				requiredVersion = versionOnDisk;
		}
		checkForUpdatesFromServer(origin, authenticate, requiredVersion);
		
		if (Files.exists(updatesFile) && versionIsNewer(versionOnDisk = ProcessInfo.getJarVersionFromUpdates(updatesFile, Misc.CONVENIENCE_SHARE_JAR), currentVersion))
		{
			completeUpdate(origin);
		}
	}


	private void completeUpdate(JFrame origin)
	{
		if (!confirmUpgrade(origin))
		{
			return;
		}
		
		try
		{
			Path currentJarFile = currentJarPath.resolve(Misc.CONVENIENCE_SHARE_JAR);
			if (Files.exists(currentJarFile))
			{
				Files.copy(currentJarFile, Paths.get(currentJarFile.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to backup current code.", e);
			return;
		}


		try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(updatesFile));)
		{
			Misc.extract(zipInputStream, currentJarPath);
		}
		catch (IOException e1)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to extract update data to " + currentJarPath, e1);
			return;
		}
		
		try
		{
			DbBackupRestore.backupDatabase(dbBackup);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.WARNING, "Unable to backup database " + currentJarPath, e);
		}
		
//		
//		// Should be an extract...
//		try
//		{
//			Files.copy(updatesFile, currentJar, StandardCopyOption.REPLACE_EXISTING);
//		}
//		catch (IOException e)
//		{
//			LogWrapper.getLogger().log(Level.INFO, "Unable to replace current code.", e);
//			return;
//		}

		restart(origin);
	}

	private boolean checkForUpdatesFromServer(JFrame origin, boolean authenticate, String requiredVersion)
	{
		try (Socket socket = new Socket(getUpdateServerUrl(), getUpdateServerPort());)
		{
			try (
				 InputStream inputStream = socket.getInputStream();
				 OutputStream outputStream = socket.getOutputStream();
				 OutputByteWriter writer = new OutputByteWriter(outputStream);)
			{
				ByteReader byteReader = new ByteReader(inputStream);

				writer.append(authenticate);
				if (authenticate)
				{
					byte[] naunce = Misc.createNaunce(Services.settings.minNaunce.get());
					byte[] encrypted = Services.keyManager.encrypt(info.getKey(), naunce);

					writer.appendVarByteArray(encrypted);
					byte[] decrypted = byteReader.readVarByteArray();
					if (!Arrays.equals(naunce, decrypted))
					{
						LogWrapper.getLogger().info("Update server failed authentication.");
						return false;
					}
				}

				String serverVersionString = byteReader.readString();
				if (!versionIsNewer(serverVersionString, requiredVersion))
				{
					LogWrapper.getLogger().info("We already have the latest version.");
					writer.append(false);

					// here we could also check the version on file...
					return false;
				}
				writer.append(true);

				
				LogWrapper.getLogger().info("Current code path is " + currentJarPath);
				LogWrapper.getLogger().info("Downloading new code to " + updatesFile);
				
				try
				{
					Files.copy(inputStream, updatesFile, StandardCopyOption.REPLACE_EXISTING);
				}
				catch (IOException e)
				{
					LogWrapper.getLogger().log(Level.INFO, "Unable to download new code.", e);
					try
					{
						Files.delete(updatesFile);
					}
					catch (IOException e1)
					{
						LogWrapper.getLogger().log(Level.INFO, "Unable to delete incomplete download.", e);
					}
					return false;
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

				return true;
			}
			catch (IOException e)
			{
				LogWrapper.getLogger().log(Level.INFO, "Unable to download update.", e);
				return false;
			}
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().info("Unable to connect to update server." + e.getMessage());
			return false;
		}
	}
	
	private static boolean versionIsNewer(String serverVersion, String currentVersion)
	{
		String[] current = currentVersion.split("\\.");
		String[] newer = serverVersion.split("\\.");
		int versionLength = Math.min(current.length, newer.length);
		for (int i = 0; i < versionLength; i++)
		{
			int newerNugget;  
			int currentNugget;
			
			try
			{
				newerNugget = Integer.parseInt(newer[i]);
				currentNugget = Integer.parseInt(current[i]);
			}
			catch (NumberFormatException ex)
			{
				LogWrapper.getLogger().info("Non-number in version: " + newer[i] + " vs " + current[i]);
				continue;
			}

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


	private void restart(JFrame origin)
	{
		UserInputWait wait = new UserInputWait();
		waitForInput(wait);
		JOptionPane.showMessageDialog(
				origin == null ? Services.notifications.getCurrentContext() : origin, 
				"Code update is complete. ConvenienceShare will now restart.", 
				"Code update.", 
				JOptionPane.INFORMATION_MESSAGE);
		wait.userInput = true;
		
		LinkedList<String> extraArgs = new LinkedList<String>();
		extraArgs.add("-r");
		extraArgs.add(dbBackup.toString());
		Main.restart(extraArgs);
	}

	private static boolean confirmUpgrade(JFrame origin)
	{
			UserInputWait wait = new UserInputWait();
			waitForInput(wait);
			boolean proceed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
					origin == null ? Services.notifications.getCurrentContext() : origin,
					"A new version of code has been found. Would you like to update now?",
					"Update ConvenienceShare",
					JOptionPane.YES_NO_OPTION);
			wait.userInput = true;
			return proceed || wait.interrupted;
	}

	public synchronized void read()
	{
		Path updateKeyPath = Services.settings.codeUpdateKey.getPath();
		LogWrapper.getLogger().info("Reading update information from " + updateKeyPath);
		try (JsonParser input = TrackObjectUtils.createParser(Files.newInputStream(updateKeyPath), false);)
		{
			info = new JsonableUpdateInfo(input);
		}
		catch (Exception e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to read update info from " + updateKeyPath, e);
			write();
		}
	}
	
	public synchronized void write()
	{
		writeTo(Services.settings.codeUpdateKey.getPath());
	}

	private void writeTo(Path file)
	{
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(file)))
		{
			info.generate(generator, null);
		}
		catch (IOException e)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to save update info", e);
		}
	}

	private static void waitForInput(UserInputWait wait)
	{
		Thread t = Thread.currentThread();
		Misc.timer.schedule(new TimerTask()
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

	private static final class UserInputWait
	{
		boolean interrupted;
		boolean userInput;
	}
}
