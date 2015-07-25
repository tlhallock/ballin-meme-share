//
///*                                                                          *
// * Copyright (C) 2015    Trever Hallock                                     *
// *                                                                          *
// * This program is free software; you can redistribute it and/or modify     *
// * it under the terms of the GNU General Public License as published by     *
// * the Free Software Foundation; either version 2 of the License, or        *
// * (at your option) any later version.                                      *
// *                                                                          *
// * This program is distributed in the hope that it will be useful,          *
// * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
// * GNU General Public License for more details.                             *
// *                                                                          *
// * You should have received a copy of the GNU General Public License along  *
// * with this program; if not, write to the Free Software Foundation, Inc.,  *
// * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.              *
// *                                                                          *
// * See LICENSE file at repo head at                                         *
// * https://github.com/tlhallock/ballin-meme-share                           *
// * or after                                                                 *
// * git clone git@github.com:tlhallock/ballin-meme-share.git                 */
//
//
//
//package org.cnv.shr.msg.key;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.security.NoSuchProviderException;
//import java.util.logging.Level;
//
//import javax.crypto.NoSuchPaddingException;
//
//import org.cnv.shr.cnctn.Communication;
//import org.cnv.shr.dmn.Services;
//import org.cnv.shr.msg.Message;
//import org.cnv.shr.util.LogWrapper;
//
//public abstract class KeyMessage extends Message
//{
//	protected KeyMessage() {}
//
//	protected KeyMessage(InputStream stream) throws IOException
//	{
//		super(stream);
//	}
//
//	@Override
//	public boolean requiresAthentication()
//	{
//		return false;
//	}
//	
//	public void fail(String message, Communication connection) throws IOException
//	{
//		try
//		{
//			connection.send(new KeyFailure(message));
//		}
//		catch (IOException e1)
//		{
//			e1.printStackTrace();
//		}
//		try
//		{
//			connection.setAuthenticated(false);
//		}
//		catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException e)
//		{
//			LogWrapper.getLogger().log(Level.SEVERE, "No provider", e);
//			Services.quiter.quit();
//		}
//		connection.finish();
//	}
//}
