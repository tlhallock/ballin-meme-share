package org.cnv.shr.track;

import java.util.List;
import java.util.TimerTask;

public class VerifierRunnable extends TimerTask
{
	VerifierRunnable() {}
	
	@Override
	public void run()
	{
		Services.tracker.trim();
		long now = System.currentTimeMillis();
		
		List<MachineEntry> cloned = Services.tracker.getEntries();
		
		for (MachineEntry entry : cloned)
		{
			if (!entry.checkConnectivity(now))
			{
				Services.tracker.remove(entry);
				continue;
			}
			if (entry.isTracker())
			{
				Services.tracker.exchange(entry.getIp(), entry.getPort());
			}
		}
	}
}
