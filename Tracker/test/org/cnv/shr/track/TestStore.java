
package org.cnv.shr.track;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.json.stream.JsonGenerator;

import org.cnv.shr.trck.CommentEntry;
import org.cnv.shr.trck.FileEntry;
import org.cnv.shr.trck.MachineEntry;
import org.cnv.shr.trck.TrackObjectUtils;
import org.cnv.shr.trck.TrackerEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class TestStore
{
	private TrackerStore store;
	private MachineEntry entry1;
	private MachineEntry entry2;
	private CommentEntry comment;
	private FileEntry sfile;
	private TrackerEntry tracker;
	
	@Before
	public void before() throws SQLException
	{
		try
		{
			Track.deleteDb();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		Track.createDb();
		store = new TrackerStore();
		entry1 = new MachineEntry("identifier", "keyStr", "127.0.0.1", 50, 52, "name1");
		entry2 = new MachineEntry("identifier2", "another key", "127.0.1.1", 50, 52, "name2");
		comment = new CommentEntry(entry1.getIdentifer(), entry2.getIdentifer(), "This is the text.", 2, System.currentTimeMillis());
		sfile = new FileEntry("0123456789abcdef", 8 * 1024);
		tracker = new TrackerEntry("192.168.1.1", 10, 11);
	}
	
	@Test
	public void testList()
	{
		store.machineFound(entry1, System.currentTimeMillis());
		StringBuilder builder = new StringBuilder();
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(new OutputStream() {
			@Override
			public void write(int b) throws IOException
			{
				builder.append((char) b);
			}
			@Override
			public void close() {}
		});)
		{
			store.listMachines(createGenerator);
		}
		
		MachineEntry another = store.getMachine(entry1.getIdentifer());
		Assert.assertNotNull(another);
		Assert.assertEquals(entry1.getIp(), another.getIp());
		
		Assert.assertTrue(builder.toString().contains("\"identifier\""));
	}

	@Test
	public void testAddComment()
	{
		store.machineFound(entry1, System.currentTimeMillis());
		store.machineFound(entry2, System.currentTimeMillis());
		
		store.postComment(comment);

		StringBuilder builder = new StringBuilder();
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException
			{
				builder.append((char) b);
			}
			@Override
			public void close() {}
		};
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listComments(entry2, createGenerator);
		}
		Assert.assertTrue(builder.toString().contains(comment.getText()));
		
		store.removeMachine(entry1);

		builder.setLength(0);
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listComments(entry2, createGenerator);
		}
		
		System.out.println(builder.toString());
		Assert.assertFalse(builder.toString().contains(comment.getText()));
	}
	
	@Test
	public void testAddFile()
	{
		store.machineFound(entry1, System.currentTimeMillis());
		store.machineFound(entry2, System.currentTimeMillis());
		
		store.machineClaims(entry1, sfile);
		
		StringBuilder builder = new StringBuilder();
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException
			{
				builder.append((char) b);
			}
			@Override
			public void close() {}
		};
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listMachines(sfile, createGenerator, 0);
		}
		Assert.assertTrue(builder.toString().contains(entry1.getIdentifer()));

		builder.setLength(0);
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listFiles(entry1, createGenerator);
		}
		
		store.debug("SFILE");
		
		Assert.assertTrue(builder.toString().contains(sfile.getChecksum()));
		
		store.machineLost(entry1, sfile);
		builder.setLength(0);
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listMachines(sfile, createGenerator, 0);
		}
		Assert.assertFalse(builder.toString().contains(entry1.getIdentifer()));
		

		builder.setLength(0);
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listFiles(entry1, createGenerator);
		}
		Assert.assertFalse(builder.toString().contains(sfile.getChecksum()));
	}
	
	@Test
	public void testTrackers()
	{
		StringBuilder builder = new StringBuilder();
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException
			{
				builder.append((char) b);
			}
			@Override
			public void close() {}
		};
		
		builder.setLength(0);
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listTrackers(createGenerator);
		}
		Assert.assertFalse(builder.toString().contains(tracker.getIp()));
		
		store.addTracker(tracker);

		builder.setLength(0);
		try (JsonGenerator createGenerator = TrackObjectUtils.createGenerator(out);)
		{
			store.listTrackers(createGenerator);
		}
		Assert.assertTrue(builder.toString().contains(tracker.getIp()));
	}
}
