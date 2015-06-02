package org.cnv.shr.dmn.trk;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.SharedFile;
import org.cnv.shr.trck.TrackerAction;
import org.cnv.shr.trck.TrackerEntry;
import org.cnv.shr.util.LogWrapper;

public class TrackerClient
{
	private TrackerEntry entry;
	
	public TrackerClient(TrackerEntry entry)
	{
		this.entry = entry;
	}
	
	TrackerConnection connect(TrackerAction action) throws IOException
	{
		return new TrackerConnection(action, entry.getIp(), entry.getBeginPort());
	}

	public void keyChanged()
	{
		try (TrackerConnection connection = connect(TrackerAction.POST_MACHINE))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
	}
	
	public void getMore()
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_TRACKERS))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
	}
	
	public void request(SharedFile file)
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_SEEDERS))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
		
	}
	
	public void synchronize()
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_MY_FILES))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
		
	}
	
	public void remove()
	{
		try (TrackerConnection connection = connect(TrackerAction.LOSE_FILE))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
	}
	
	public void claim(SharedFile file)
	{
		try (TrackerConnection connection = connect(TrackerAction.CLAIM_FILE))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
	}
	
	Iterator<Machine> browse(TrackerEntry entry)
	{
		try (TrackerConnection connection = connect(TrackerAction.LIST_ALL_MACHINES))
		{
			
		}
		catch (IOException ex)
		{
			LogWrapper.getLogger().log(Level.INFO, "Unable to connect", ex);
		}
		
//		LIST_RATINGS
//		POST_COMMENT
		return null;
	}

	public TrackerEntry getEntry()
	{
		return entry;
	}
}
