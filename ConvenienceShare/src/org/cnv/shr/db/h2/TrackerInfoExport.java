package org.cnv.shr.db.h2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;

import javax.json.stream.JsonGenerator;

import org.cnv.shr.dmn.Services;
import org.cnv.shr.dmn.trk.ClientTrackerClient;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.util.KeyPairObject;
import org.cnv.shr.util.LogWrapper;

public class TrackerInfoExport
{
	public static void exportTrackerInfo(Path f) throws IOException
	{
		LogWrapper.getLogger().info("Backing up the database.");
		
		try (JsonGenerator generator = TrackObjectUtils.createGenerator(Files.newOutputStream(f), true);)
		{
			generator.writeStartObject();
			LogWrapper.getLogger().info("Writing machines.");
			generator.writeStartArray("machines");
			try (DbIterator<Machine> listLocals = DbMachines.listRemoteMachines();)
			{
				while (listLocals.hasNext())
				{
					Machine next = listLocals.next();
					PublicKey key = DbKeys.getKey(next);
					new MachineEntry(next.getIdentifier(),
							KeyPairObject.serialize(key),
							next.getIp(),
							next.getPort(),
							next.getName()).generate(generator, null);
				}
			}
			generator.writeEnd();


			LogWrapper.getLogger().info("Writing trackers.");
			generator.writeStartArray("trackers");
			Iterable<ClientTrackerClient> trackers = Services.trackers.getClients();
			for (ClientTrackerClient client : trackers)
			{
				client.getEntry().generate(generator);
			}
			generator.writeEnd();
			
			generator.writeEnd();

			Services.notifications.localsChanged();
		}
		LogWrapper.getLogger().info("Database backup complete.");
	}
}
