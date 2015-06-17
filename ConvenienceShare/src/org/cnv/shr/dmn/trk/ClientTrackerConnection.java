
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



package org.cnv.shr.dmn.trk;

import java.io.IOException;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.util.Misc;

import de.flexiprovider.core.rsa.RSAPublicKey;

public class ClientTrackerConnection extends TrackerConnection
{
	RSAPublicKey publicKey;
	
	ClientTrackerConnection(String url, int port) throws IOException
	{
		super(url, port);
	}

	@Override
	protected MachineEntry getLocalMachine()
	{
		publicKey = Services.keyManager.getPublicKey();
		return new MachineEntry(
				Services.settings.machineIdentifier.get(),
				publicKey,
				"not used.",
				Services.settings.servePortBeginE.get(),
				Services.settings.servePortBeginE.get() + Services.settings.numHandlers.get(),
				Services.settings.machineName.get());
	}

	@Override
	protected void sendDecryptedNaunce(byte[] naunceRequest, RSAPublicKey publicKey2)
	{
		if (publicKey2 == null)
		{
			publicKey2 = publicKey;
		}
		generator.write("decrypted", Misc.format(Services.keyManager.decrypt(publicKey, naunceRequest)));
	}
}
