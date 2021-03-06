package org.cnv.shr.test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.LinkedList;

import org.cnv.shr.db.h2.DbFiles;
import org.cnv.shr.db.h2.DbMachines;
import org.cnv.shr.db.h2.DbPaths2;
import org.cnv.shr.db.h2.DbRoots;
import org.cnv.shr.db.h2.SharingState;
import org.cnv.shr.dmn.Services;
import org.cnv.shr.gui.UserActions;
import org.cnv.shr.mdl.Machine;
import org.cnv.shr.mdl.RootDirectory;

public class TestActions
{
	private static LinkedList<TestAction> actions = new LinkedList<>();
	
	public static abstract class TestAction implements Serializable
	{
		public abstract void perform() throws Exception;
	}

//	public static TestAction REMOVE_MACHINE = new TestAction("REMOVE_MACHINE")
//	{
//		public void perform()
//		{
//			GuiActions.removeMachine(remote);
//		}
//	};
	
	public static class AddMachine extends TestAction
	{
		private String url;
		
		public AddMachine(String url)
		{
			this.url = url;
		}
		
		@Override
		public void perform()
		{
			UserActions.addMachine(url, new org.cnv.shr.gui.AddMachine.AddMachineParams(true));
		}
	};
	
	public static class Die extends TestAction
	{
		@Override
		public void perform() throws Exception
		{
			Services.quiter.quit();
		}
	};
	public static class SYNC_ROOTS extends TestAction
	{
		private String ident;
		
		public SYNC_ROOTS(String ident)
		{
			this.ident = ident;
		}
		@Override
		public void perform()
		{
			UserActions.syncRoots(null, DbMachines.getMachine(ident));
		}
	};
	public static class FIND_MACHINES extends TestAction
	{
		private String ident;
		
		@Override
		public void perform()
		{
			UserActions.findMachines(null, DbMachines.getMachine(ident));
		}
	};
	public static class SYNC_ALL_LOCAL extends TestAction
	{
		@Override
		public void perform()
		{
			UserActions.syncAllLocals(null);
		}
	};
	public static class SYNC_LOCAL extends TestAction
	{
		String local;
		
		SYNC_LOCAL(String local)
		{
			this.local = local;
		}
		
		@Override
		public void perform() throws IOException
		{
			UserActions.userSync(null, DbRoots.getLocal(Paths.get(local)), null);
		}
	};
	public static class ADD_LOCAL extends TestAction
	{
		String local;
		String name;
		
		ADD_LOCAL(String local, String name)
		{
			this.local = local;
			this.name = name;
		}
		
		@Override
		public void perform()
		{
			UserActions.addLocalImmediately(Paths.get(local), name, false);
		}
	};
	public static class SYNC_REMOTE extends TestAction
	{
		String ident;
		String name;
		
		SYNC_REMOTE(String ident, String name)
		{
			this.name = name;
			this.ident = ident;
		}
		
		@Override
		public void perform()
		{
			Machine machine = DbMachines.getMachine(ident);
			RootDirectory root = DbRoots.getRoot(machine, name);
			root.synchronize(null, null);
		}
	};
	public static class REMOVE_LOCAL extends TestAction
	{
		String local;
		
		public REMOVE_LOCAL(String local)
		{
			this.local = local;
		}
		
		@Override
		public void perform() throws IOException
		{
			UserActions.remove(DbRoots.getLocal(Paths.get(local)));
		}
	};
	public static class SHARE_WITH extends TestAction
	{
		private String ident;
		private boolean share;
		
		public SHARE_WITH(String ident, boolean share)
		{
			this.ident = ident;
			this.share = share;
		}
		
		@Override
		public void perform()
		{
			UserActions.shareWith(DbMachines.getMachine(ident), share ? SharingState.DOWNLOADABLE : SharingState.DO_NOT_SHARE);
		}
	};
	public static class DOWNLOAD extends TestAction
	{
		String ident;
		String name;
		String path;
		
		public DOWNLOAD(String ident, String name, String path)
		{
			this.ident = ident;
			this.name = name;
			this.path = path;
		}
		
		@Override
		public void perform()
		{
			UserActions.download(
					DbFiles.getFile(
							DbPaths2.findFilePath(
									DbRoots.getRoot(
											DbMachines.getMachine(ident), name), path)));
		}
	};
	
	public static void run(ObjectInputStream reader) throws Exception
	{
		TestAction action;
		while ((action = (TestAction) reader.readObject()) != null)
		{
			action.perform();
		}
	}
}
